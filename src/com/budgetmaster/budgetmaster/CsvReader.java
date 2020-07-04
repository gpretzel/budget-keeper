package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingBiFunction;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;


public final class CsvReader<T> {
    public CsvReader() {
        setFailFast(true);
    }

    public Stream<T> readCsv(Path file) throws IOException {
        final Collection<T> result = new ArrayList<>();

        BiConsumer<String, Exception> reportError = (msg, ex) -> {
            try {
                throw new CsvReaderException(String.format("Error %s record #%d",
                        msg, result.size() + 1), ex);
            } catch (RuntimeException ex2) {
                if (failFast) {
                    throw ex2;
                }
                ex.printStackTrace();
            }
        };

        try (Reader in = Files.newBufferedReader(file)) {
            CSVParser parser = csvFormat.parse(in);
            if (headerNamesConsumer != null) {
                headerNamesConsumer.accept(parser.getHeaderNames());
            }
            for (CSVRecord record : parser) {
                try {
                    result.add(conv.apply(record, reportError));
                } catch (DateTimeParseException ex) {
                    reportError.accept("parsing date value", ex);
                } catch (CsvReaderException ex) {
                    throw ex;
                } catch (Exception ex) {
                    reportError.accept("processing", ex);
                }
            }
        }

        return result.stream();
    }

    public CsvReader<T> setFormat(CSVFormat v) {
        csvFormat = v;
        return this;
    }

    public CsvReader<T> setFailFast(boolean v) {
        failFast = v;
        return this;
    }

    public CsvReader<T> setThrowingConv(
            ThrowingBiFunction<CSVRecord, BiConsumer<String, Exception>, T> v) {
        setConv(ThrowingBiFunction.toBiFunction(v));
        return this;
    }

    public CsvReader<T> setConv(
            BiFunction<CSVRecord, BiConsumer<String, Exception>, T> v) {
        conv = v;
        return this;
    }

    public CsvReader<T> setHeaderNamesConsumer(Consumer<List<String>> v) {
        headerNamesConsumer = v;
        return this;
    }

    public static String[] read(CSVRecord csvRecord) {
        return StreamSupport.stream(csvRecord.spliterator(), false).toArray(
                String[]::new);
    }

    private CSVFormat csvFormat;
    private BiFunction<CSVRecord, BiConsumer<String, Exception>, T> conv;
    private Consumer<List<String>> headerNamesConsumer;
    private boolean failFast;

    private static class CsvReaderException extends RuntimeException {
        public CsvReaderException(String msg, Throwable t) {
            super(msg, t);
        }
    }
}
