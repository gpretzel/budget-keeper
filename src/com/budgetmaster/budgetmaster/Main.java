package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
        description = "Extracts and filters transactions from different banck account statements",
        name = "budgetmaster", mixinStandardHelpOptions = true,
        version = "budgetmaster 0.1")
final class Main implements Callable<Integer> {
    public static void main(String... args) throws Exception {
        LogManager.getLogManager().readConfiguration();
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            return workload();
        } catch (Functional.ExceptionBox ex) {
            throw (Exception)ex.getCause();
        }
    }

    private static Path[] explodePaths(Path[] paths) throws IOException {
        List<Path> result = new ArrayList<>();
        for (Path path: paths) {
            if (Files.isDirectory(path)) {
                LOGGER.finer(String.format("Expand [%s] folder", path));
                Files.walk(path)
                    .filter(Files::isRegularFile)
                    .peek(v -> LOGGER.finer(v.toString()))
                    .forEachOrdered(result::add);
            } else {
                result.add(path);
            }
        }

        return result.toArray(Path[]::new);
    }

    private int workload() throws Exception {
        LOGGER.finer(String.format("Read [%s] config file", configXmlFile));
        Document configXml = Util.readXml(configXmlFile);

        StatementReaderBuilder rsfb = new StatementReaderBuilder();
        MainRecordsProcessorBuilder mrpb = new MainRecordsProcessorBuilder();

        if (variables != null) {
            mrpb.setVaribales(Stream.of(variables).map(str -> {
                int sepIdx = str.indexOf('=');
                if (sepIdx < 0) {
                    throw new IllegalArgumentException(String.format(
                            "Invalid variable value: [%s]. Missing '=' character",
                            str));
                }
                String name = str.substring(0, sepIdx);
                String value = str.substring(sepIdx + 1);
                return Map.entry(name, value);
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        }

        if (discardToCsvFile != null) {
            mrpb.collectDiscardedRecords(true);
        }

        mrpb.setMappersOrder(actionIds);

        Path[] filteredStatementPaths = explodePaths(statementPaths);

        statementCfg = rsfb.createFromXml(configXml);
        UnaryOperator<Stream<Record>> recordsMapper = mrpb.createFromXml(
                configXml.getDocumentElement());

        long totalRecords = -1;
        long keptRecords = -1;
        long processed = -1;
        long failed = -1;
        long ignored = -1;

        switch (maxStatementHarvestJobCount) {
            case 0:
                statementHarvestJobExecutor = Executors.newWorkStealingPool();
                break;

            case 1:
                statementHarvestJobExecutor = Executors.newSingleThreadExecutor();
                break;

            default:
                statementHarvestJobExecutor = Executors.newFixedThreadPool(
                        maxStatementHarvestJobCount);
                break;
        }

        try {
            List<Future<List<Record>>> statements = statementHarvestJobExecutor.invokeAll(
                    Stream.of(filteredStatementPaths)
                            .map(StatementHarvestJob::new)
                            .collect(Collectors.toList()));
            
            statements.stream()
                    .filter(Predicate.not(Future::isCancelled))
                    .forEachOrdered(f -> {
                        try {
                            f.get();
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                    });
            
            failed = statements.stream()
                    .filter(Predicate.not(Future::isCancelled))
                    .filter(f -> {
                        try {
                            f.get();
                            return false;
                        } catch (InterruptedException | ExecutionException ex) {
                            return true;
                        }
                    })
                    .count();

            ignored = statements.stream()
                    .filter(Predicate.not(Future::isCancelled))
                    .filter(f -> {
                        try {
                            return f.get() == null;
                        } catch (InterruptedException | ExecutionException ex) {
                            return false;
                        }
                    })
                    .count();

            List<List<Record>> harvestedStatements = statements.stream()
                    .filter(Predicate.not(Future::isCancelled))
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (InterruptedException | ExecutionException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            processed = harvestedStatements.size();

            List<Record> records = harvestedStatements.stream().flatMap(
                    x -> x.stream()).collect(Collectors.toList());

            totalRecords = keptRecords = records.size();

            if (statements.stream().anyMatch(Future::isCancelled) || failed > 0) {
                if (failFast) {
                    return 1;
                }
            }

            if (findOverlappedStatements(records.stream())) {
                if (failFast) {
                    return 1;
                }
            }

            records = recordsMapper.apply(records.stream()).collect(
                    Collectors.toList());

            keptRecords = records.size();

            if (saveToCsvFile != null) {
                Util.saveToCsvFile(saveToCsvFile, records.stream());
            }

            if (discardToCsvFile != null) {
                Util.saveToCsvFile(discardToCsvFile, mrpb.getDiscardedRecords());
            }

            return 0;
        } finally {
            final long notProcessed = filteredStatementPaths.length - (processed
                    + failed + ignored);
            System.out.println(String.format(
                    "Total input files count: %d; success: %d; failed: %d; unrecognized: %d; skipped: %d",
                    filteredStatementPaths.length, processed, failed, ignored,
                    notProcessed));
            System.out.println(String.format(
                    "Total records harvested: %d; kept: %d; discarded: %d",
                    totalRecords, keptRecords, totalRecords - keptRecords));
        }
    }

    private boolean findOverlappedStatements(Stream<Record> records) {
        if (!checkOverlappedStatements) {
            return false;
        }
        var overlappedStatements = new StatementOverlapFinder().apply(records);
        if (overlappedStatements != null && !overlappedStatements.isEmpty()) {
            for (var pair : overlappedStatements) {
                System.err.println(String.format(
                        "Overlapped statements: [%s] [%s]", pair.getKey(),
                        pair.getValue()));
            }
            return true;
        }
        return false;
    }

    private final class StatementHarvestJob implements Callable<List<Record>> {
        StatementHarvestJob(Path src) {
            this.statement = statementCfg.apply(src);
            if (statement == null) {
                LOGGER.warning(String.format(
                        "Failed to find harvester for [%s] input file", src));
            }
            this.src = src;
        }

        @Override
        public List<Record> call() throws Exception {
            try {
                LOGGER.finer(String.format(
                        "Process [%s] input file with %s harvester", src,
                        statement.getId()));
                if (dryRun) {
                    return Collections.emptyList();
                }
                return statement.get().collect(Collectors.toList());
            } catch (Exception ex) {
                String errMsg = String.format("Error harvesting [%s] input file",
                        src);
                LOGGER.severe(String.format("%s: %s", errMsg, ex.getMessage()));
                System.out.println(errMsg);
                ex.printStackTrace();

                if (failFast) {
                    statementHarvestJobExecutor.shutdownNow();
                }

                throw ex;
            }
        }

        private final Statement statement;
        private final Path src;
    }

    @Option(names = {"-c", "--config"},
            required = true,
            description = "path to config XML file")
    private Path configXmlFile;

    @Option(names = {"-j", "--jobs"},
            description = "maximum number of bank statement harvesting jobs")
    private int maxStatementHarvestJobCount;

    @Option(names = {"-s", "--save-collector"},
            description = "path to CSV file where to save filtered records")
    private Path saveToCsvFile;

    @Option(names = {"-r", "--discard-collector"},
            description = "path to CSV file where to save discarded records")
    private Path discardToCsvFile;

    @Option(names = {"-d", "--dry-run"}, description = "dry run")
    private boolean dryRun;

    @Option(names = {"--check-overlapped-statements"}, description = "dry run")
    private boolean checkOverlappedStatements;

    @Option(names = {"-i", "--action-ids"},
            description = "ordered list of IDs of actions to execute from the config file")
    private String[] actionIds;

    @Option(names = {"-e", "--set-variable"},
            description = "set variable")
    private String[] variables;

    @Option(names = {"-a", "--fail-fast"},
            description = "abort after the first encountered error")
    private boolean failFast;

    @Parameters(paramLabel = "FILE",
            description = "one ore more statemnet files/directories to process")
    private Path[] statementPaths;

    private Function<Path, Statement> statementCfg;
    private ExecutorService statementHarvestJobExecutor;

    private static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());
}
