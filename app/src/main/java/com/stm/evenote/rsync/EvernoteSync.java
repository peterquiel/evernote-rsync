package com.stm.evenote.rsync;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private static final Pattern tokenExpiresPattern = Pattern.compile("\\\"exp\\\":\\s*(?<exp>\\d*)");
  private static Pattern monoAuthnTokenPattern =
    Pattern.compile("\\\"mono_authn_token\\\":\\s*\\\"(?<authnToken>[^\\\"]+)\\\"");

  @CommandLine.Option(names = {"-d", "--directory"},
    description = "Local directory to sync to and from. Default is current working directory")
  String localDirectory = Paths.get(".").toAbsolutePath().normalize().toString();

  @CommandLine.Option(names = {"-s", "--stacks"},
    description = "A list of Evernote stack names its attachments should be synced.")
  List<String> stacks = Collections.emptyList();

  @CommandLine.Option(names = {"-n", "--notebooks"},
    description = "A list of Evernote notebook names its attachments should be synced.")
  List<String> notebooks = Collections.emptyList();

  @CommandLine.Option(names = {"-jt", "--jwt-token"},
    description = "Evernote jwt token; read readme in order to know how to extract the token from web client login" +
      " " +
      "process", required = true)
  String jwtToken;

  @CommandLine.Option(names = {"-es", "--evernote-service"},
    description = "Evernote service to use. default is PRODUCTION; possible values: ${COMPLETION-CANDIDATES}")
  EvernoteService evernoteService = EvernoteService.PRODUCTION;

  @CommandLine.Option(names = {"--delete"},
    description = "Delete empty local directories or local files not found in Evernote. Default false.")
  boolean delete = false;

  @CommandLine.Option(names = {"-dr", "--dryRun"},
    description = "Dry Run: Log file operations instead of applying them.")
  boolean dryRun = false;

  @CommandLine.Option(names = {"-v", "--version"}, versionHelp = true,
    description = "print version information and exit")
  boolean versionRequested = false;

  @CommandLine.Option(names = {"--verbose"}, description = "detailed log output")
  boolean verbose = false;

  @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
  boolean usageHelpRequested = false;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new EvernoteSync()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    final Optional<String> autTokenOptional = extractAccessTokenFrom(this.jwtToken);
    if (autTokenOptional.isEmpty()) {
      System.exit(1);
    }

    final String authnToken = autTokenOptional.get();
    logger.info(
      "Authn Token successfully extracted. Connecting to {} with token: {}...",
      evernoteService.getHost(),
      authnToken.substring(0, 20)
    );

    if (this.notebooks.isEmpty() && this.stacks.isEmpty()) {
      System.out.println("Missing Evernote stacks and/or notebooks.");
      new CommandLine(this).usage(System.out);
      return 1;
    }

    final ClientFactory clientFactory = new ClientFactory(new EvernoteAuth(evernoteService, authnToken));

    final NoteStoreClient noteClient = clientFactory.createNoteStoreClient();

    var evernoteSyncFileFactory = new EvernoteSyncFileFactory(noteClient).loadAllNotebooks();

    final SyncFiles evernoteSyncFiles = evernoteSyncFileFactory.createForStacks(this.stacks)
      .merge(evernoteSyncFileFactory.createForNotebooks(this.notebooks));

    final SyncFiles localFiles = new LocalSyncFileFactory().createFor(new File(this.localDirectory));

    logger.info("evernote: \n{}", evernoteSyncFiles);
    logger.info("local: \n{}", localFiles);

    var operationFactory = createOperationFactory();
    localFiles.migrateTo(
        evernoteSyncFiles,
        operationFactory
      )
      .forEach(Operation::execute);

    return 0;
  }

  private OperationFactory createOperationFactory() {
    if (this.dryRun) {
      return new DryRunOperationFactory(this.localDirectory);
    } else {
      return new FileSystemOperationFactory(this.localDirectory, this.delete);
    }
  }

  Optional<String> extractAccessTokenFrom(String jwtToken) {
    try {
      return Arrays.stream(jwtToken.split("\\."))
        .map(String::trim)
        .map(s -> s.replaceAll("\\s", ""))
        .map(Base64.getDecoder()::decode)
        .map(String::new)
        .map(this::extractMonoAuthnToken)
        .filter(Optional::isPresent)
        .flatMap(Optional::stream)
        .findFirst();
    } catch (Exception e) {
      logger.error(
        "Could not extract auth token from jwt token. Use `https://jwt.io/` and verify that you find an " +
          "mono_authn_token in the token.",
        e
      );
      return Optional.empty();
    }
  }

  Optional<String> extractMonoAuthnToken(String candidate) {
    final Matcher matcher = monoAuthnTokenPattern.matcher(candidate);
    if (matcher.find()) {
      final Matcher expMatcher = tokenExpiresPattern.matcher(candidate);
      if (expMatcher.find()) {
        final String expires = expMatcher.group("exp");

        final Date expireDate = new Date(Long.parseLong(expires) * 1000);
        if (expireDate.before(new Date())) {
          logger.warn(
            "Authn Token expired already at {}. Please re-extract jwt token from evernote web client!",
            expireDate
          );
        } else {
          logger.info("Authn Token expires: {}", expireDate);
        }
      }
      return Optional.of(matcher.group("authnToken"));
    }
    return Optional.empty();
  }
}