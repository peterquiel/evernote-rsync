package com.stm.evenote.rsync

import com.evernote.auth.EvernoteAuth
import com.evernote.auth.EvernoteService
import com.evernote.clients.ClientFactory
import com.evernote.clients.NoteStoreClient
import com.stm.evenote.rsync.model.EvernoteSyncFileFactory
import com.stm.evenote.rsync.model.LocalSyncFileFactory
import com.stm.evenote.rsync.model.Operation
import com.stm.evenote.rsync.model.OperationFactory
import com.stm.evenote.rsync.opfactory.DryRunOperationFactory
import com.stm.evenote.rsync.opfactory.FileSystemOperationFactory
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.function.Consumer
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "Evernote Rsync",
    mixinStandardHelpOptions = true,
    showAtFileInUsageHelp = true,
    version = ["Evernote Rsync Version: 0.6"]
)
class EvernoteSync : Callable<Int> {
    @CommandLine.Option(
        names = ["-d", "--directory"],
        description = ["Local directory to sync to. Evernote attachments are written in this directory. Default is current" +
                " working directory"]
    )
    var localDirectory = Paths.get(".").toAbsolutePath().normalize().toString()

    @CommandLine.Option(
        names = ["-s", "--stacks"],
        description = ["A list of Evernote stack names its attachments should be synced."]
    )
    var stacks: List<String?> = emptyList<String>()

    @CommandLine.Option(
        names = ["-n", "--notebooks"],
        description = ["A list of Evernote notebook names its attachments should be synced."]
    )
    var notebooks: List<String?> = emptyList<String>()

    @CommandLine.Option(
        names = ["-t", "--token"],
        description = ["Evernote access token; read readme in order to know how to extract the token from web client login" +
                " process"],
        interactive = true,
        required = true
    )
    var token: String? = null

    @CommandLine.Option(
        names = ["-es", "--evernote-service"],
        description = ["Evernote service to use. Default is PRODUCTION; possible values: \${COMPLETION-CANDIDATES}"]
    )
    var evernoteService = EvernoteService.PRODUCTION

    @CommandLine.Option(
        names = ["--delete"],
        description = ["Delete empty local directories or local files not found in Evernote. Default false."]
    )
    var delete = false

    @CommandLine.Option(
        names = ["-de", "--delete-excludes"],
        description = ["Regular expression to exclude files or folders for deletion."]
    )
    var excludeDelete = emptyList<String>()

    @CommandLine.Option(
        names = ["-dr", "--dryRun"],
        description = ["Dry Run: Log file operations instead of applying them."]
    )
    var dryRun = false

    @CommandLine.Option(names = ["--verbose"], description = ["detailed log output"])
    var verbose = false

    @Suppress("unused")
    @CommandLine.Option(
        names = ["-v", "--version"],
        versionHelp = true,
        description = ["print version information and exit"]
    )
    var versionRequested = false

    @Suppress("unused")
    @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["display this help message"])
    var usageHelpRequested = false

    override fun call(): Int {
        logger.info("Connecting to ${evernoteService.host} using given token")
        if (notebooks.isEmpty() && stacks.isEmpty()) {
            logger.error("Missing Evernote stacks and/or notebooks.")
            CommandLine(this).usage(System.out)
            return 1
        }
        if (delete && !excludeDelete.isEmpty()) {
            logger.info(
                "Delete is active, following exclude patterns defined: '{}'",
                java.lang.String.join("','", excludeDelete)
            )
        }
        val clientFactory = ClientFactory(EvernoteAuth(evernoteService, token))
        val noteClient = createNoteStoreClient(clientFactory)
        val evernoteSyncFileFactory = EvernoteSyncFileFactory(noteClient!!).loadAllNotebooks()
        val evernoteSyncFiles = evernoteSyncFileFactory.createForStacks(stacks)
            .merge(evernoteSyncFileFactory.createForNotebooks(notebooks))
        val localFiles = LocalSyncFileFactory().createFor(File(localDirectory))
        if (verbose) {
            logger.info("Evernote Attachments: \n${evernoteSyncFiles}")
            logger.info("Local Files System: \n${localFiles}")
        }
        val operationFactory = createOperationFactory()
        localFiles.migrateTo(
            evernoteSyncFiles,
            operationFactory
        )
            .forEach(Consumer { obj: Operation -> obj.execute() })
        return 0
    }

    private fun createNoteStoreClient(clientFactory: ClientFactory): NoteStoreClient? {
        return try {
            clientFactory.createNoteStoreClient()
        } catch (e: Exception) {
            logger.error("Could not connect to evernote with probably invalid token: '${token}'.", e)
            exitProcess(1)
            null
        }
    }

    private fun createOperationFactory(): OperationFactory {
        return if (dryRun) {
            DryRunOperationFactory(localDirectory, delete, excludeDelete)
        } else {
            FileSystemOperationFactory(localDirectory, delete, excludeDelete)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EvernoteSync::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(EvernoteSync()).execute(*args)
            exitProcess(exitCode)
        }
    }
}