package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingBiFunction;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


public class CsvReader<T> {
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
            Iterable<CSVRecord> records = csvParser.parse(in);
            for (CSVRecord record : records) {
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

    public CsvReader<T> setParser(CSVFormat v) {
        csvParser = v;
        return this;
    }

    public CsvReader<T> setFailFast(boolean v) {
        failFast = v;
        return this;
    }

    public CsvReader<T> setConv(
            ThrowingBiFunction<CSVRecord, BiConsumer<String, Exception>, T> v) {
        conv = ThrowingBiFunction.toBiFunction(v);
        return this;
    }

    private CSVFormat csvParser;
    private BiFunction<CSVRecord, BiConsumer<String, Exception>, T> conv;
    private boolean failFast;

    private static class CsvReaderException extends RuntimeException {
        public CsvReaderException(String msg, Throwable t) {
            super(msg, t);
        }
    }
}
