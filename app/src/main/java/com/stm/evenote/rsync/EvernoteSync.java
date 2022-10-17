package com.stm.evenote.rsync;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.stm.evenote.rsync.model.EvernoteSyncFileFactory;
import com.stm.evenote.rsync.model.LocalSyncFileFactory;
import com.stm.evenote.rsync.model.Operation;
import com.stm.evenote.rsync.model.OperationFactory;
import com.stm.evenote.rsync.model.SyncFiles;
import com.stm.evenote.rsync.opfactory.DryRunOperationFactory;
import com.stm.evenote.rsync.opfactory.FileSystemOperationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "Evernote Rsync",
  mixinStandardHelpOptions = true,
  showAtFileInUsageHelp = true,
  version = "Evernote Sync Version: 0.3")
public class EvernoteSync implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(EvernoteSync.class);

  @CommandLine.Option(names = {"-d", "--directory"},
    description = "Local directory to sync to and from. Default is current working directory")
  String localDirectory = Paths.get(".").toAbsolutePath().normalize().toString();

  @CommandLine.Option(names = {"-s", "--stacks"},
    description = "A list of Evernote stack names its attachments should be synced.")
  List<String> stacks = Collections.emptyList();

  @CommandLine.Option(names = {"-n", "--notebooks"},
    description = "A list of Evernote notebook names its attachments should be synced.")
  List<String> notebooks = Collections.emptyList();

  @CommandLine.Option(names = {"-t", "--token"},
    description = "Evernote access token; read readme in order to know how to extract the token from web client login" +
      " process", interactive = true, required = true)
  String token;

  @CommandLine.Option(names = {"-es", "--evernote-service"},
    description = "Evernote service to use. default is PRODUCTION; possible values: ${COMPLETION-CANDIDATES}")
  EvernoteService evernoteService = EvernoteService.PRODUCTION;

  @CommandLine.Option(names = {"--delete"},
    description = "Delete empty local directories or local files not found in Evernote. Default false.")
  boolean delete = false;

  @CommandLine.Option(names = {"-de", "--delete-excludes"},
    description = "Regular expression to exclude files or folders for deletion.")
  List<String> excludeDelete = Collections.emptyList();

  @CommandLine.Option(names = {"-dr", "--dryRun"},
    description = "Dry Run: Log file operations instead of applying them.")
  boolean dryRun = false;

  @CommandLine.Option(names = {"--verbose"}, description = "detailed log output")
  boolean verbose = false;

  @SuppressWarnings("unused")
  @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true,
    description = "print version information and exit")
  boolean versionRequested = false;

  @SuppressWarnings("unused")
  @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
  boolean usageHelpRequested = false;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new EvernoteSync()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    logger.info("Connecting to {} using given token", evernoteService.getHost());
    if (this.notebooks.isEmpty() && this.stacks.isEmpty()) {
      System.out.println("Missing Evernote stacks and/or notebooks.");
      new CommandLine(this).usage(System.out);
      return 1;
    }

    if (this.delete && !this.excludeDelete.isEmpty()) {
      logger.info("Delete is active, following exclude patterns defined: '{}'",
        String.join("','", this.excludeDelete)
      );
    }

    final ClientFactory clientFactory = new ClientFactory(new EvernoteAuth(evernoteService, token));

    NoteStoreClient noteClient = createNoteStoreClient(clientFactory);

    var evernoteSyncFileFactory = new EvernoteSyncFileFactory(noteClient).loadAllNotebooks();

    final SyncFiles evernoteSyncFiles = evernoteSyncFileFactory.createForStacks(this.stacks)
      .merge(evernoteSyncFileFactory.createForNotebooks(this.notebooks));

    final SyncFiles localFiles = new LocalSyncFileFactory().createFor(new File(this.localDirectory));

    if (verbose) {
      logger.info("Evernote Attachments: \n{}", evernoteSyncFiles);
      logger.info("Local Files System: \n{}", localFiles);
    }

    var operationFactory = createOperationFactory();
    localFiles.migrateTo(
        evernoteSyncFiles,
        operationFactory
      )
      .forEach(Operation::execute);

    return 0;
  }

  private NoteStoreClient createNoteStoreClient(ClientFactory clientFactory) {
    try {
      return clientFactory.createNoteStoreClient();
    } catch(Exception e) {
      logger.error("Could not connect to evernote with probably invalid token: '{}'.", token, e);
      System.exit(1);
      return null;
    }
  }

  private OperationFactory createOperationFactory() {
    if (this.dryRun) {
      return new DryRunOperationFactory(this.localDirectory, this.delete, this.excludeDelete);
    } else {
      return new FileSystemOperationFactory(this.localDirectory, this.delete, this.excludeDelete);
    }
  }
}
