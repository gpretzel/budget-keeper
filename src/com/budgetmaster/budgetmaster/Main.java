package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.LogManager;
import java.util.logging.Logger;
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
public class Main implements Callable<Integer> {

    private static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());
    
    @Option(names = {"-c", "--config"}, description = "path to config XML file")
    private Path configXmlFile;

    @Option(names = {"-o", "--output"}, description = "path to output CSV file")
    private Path outputCsvFile;
    
    @Option(names = {"-d", "--dry-run"}, description = "dry run")
    private boolean dryRun;

    @Parameters(paramLabel = "FILE",
            description = "one ore more statemnet files/directories to process")
    private Path[] statementPaths;
    
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

    public static void main(String... args) throws Exception {
        LogManager.getLogManager().readConfiguration();
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
    
    private Stream<Record> harvestStatement(Statement statement,
            UnaryOperator<Record> mapper, Path path) throws Exception {
        try {
            Stream<Record> records = statement.get();
            return records.map(mapper).filter(Objects::nonNull);
        } catch (Throwable t) {
            if (t instanceof Exception) {
                throw (Exception)t;
            }
            throw new RuntimeException(t);
        }
    }
    
    @Override
    public Integer call() throws Exception {
        LOGGER.finer(String.format("Read [%s] config file", configXmlFile));
        Document configXml = Util.readXml(configXmlFile);

        StatementReaderBuilder rsfb = new StatementReaderBuilder();
        RecordMapperBuilder rmb = new RecordMapperBuilder();

        Path[] filteredStatementPaths = explodePaths(statementPaths);

        Function<Path, Statement> statementCfg = rsfb.createFromXml(configXml);
        UnaryOperator<Record> recordMapper = rmb.createFromXml(configXml);
        
        List<Stream<Record>> records = new ArrayList<>();

        int processed = 0;
        int failed = 0;
        int ignored = 0;
        try {
            for (Path path : filteredStatementPaths) {
                Statement statement = statementCfg.apply(path);
                if (statement == null) {
                    ignored++;
                    LOGGER.warning(String.format(
                            "Failed to find harvester for [%s] input file", path));
                } else {
                    try {
                        LOGGER.finer(String.format(
                                "Process [%s] input file with %s harvester",
                                path, statement.getId()));
                        if (!dryRun) {
                            records.add(
                                    harvestStatement(statement, recordMapper,
                                            path));
                        }
                        processed++;
                    } catch (Exception ex) {
                        failed++;
                        LOGGER.severe(String.format(
                                "Error harvesting [%s] input file: %s", path,
                                ex.getMessage()));
                        ex.printStackTrace();
                    }
                }
            }
            
            if (outputCsvFile != null) {
                new RecordCsvSerializer().saveToFile(records.stream().flatMap(
                        s -> s), outputCsvFile);
            }
                
            return 0;
        } finally {
            int notProcessed = filteredStatementPaths.length - (processed
                    + failed + ignored);
            System.out.println(String.format(
                    "Total input files count: %d; success: %d; failed: %d; unrecognized: %d; not processed: %d",
                    filteredStatementPaths.length, processed, failed, ignored,
                    notProcessed));
        }
    }
}
