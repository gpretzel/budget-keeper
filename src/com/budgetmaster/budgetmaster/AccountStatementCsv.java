package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


public abstract class AccountStatementCsv implements RecordSupplier {
    @Override
    final public Stream<Record> read(Path csvFilePath) throws IOException {
        final Map<RecordBuilder.Setter, Enum<?>> fieldMapper = fieldMapper();

        final List<Record> result = new ArrayList<>();

        BiConsumer<String, Exception> reportError = (msg, ex) -> {
            throw new RuntimeException(String.format("Error %s record #%d", msg,
                    result.size() + 1), ex);
        };

        try (Reader in = Files.newBufferedReader(csvFilePath)) {
            Iterable<CSVRecord> records = initParser().parse(in);
            for (CSVRecord record : records) {
                RecordBuilder rb = new RecordBuilder();
                for (var fieldEntry : fieldMapper.entrySet()) {
                    final String value = record.get(fieldEntry.getValue().name());
                    switch (fieldEntry.getKey()) {
                        case TransactionDate:
                        case PostingDate:
                            try {
                                final LocalDate date = LocalDate.parse(value,
                                        recordDateTimeFormatter());
                                setterHelper(fieldEntry.getKey().method, rb, date);
                            } catch (DateTimeParseException ex) {
                                reportError.accept(String.format(
                                        "parsing date (%s) of", value), ex);
                            }
                            break;

                        case Currency:
                            try {
                                final Currency currency = Currency.getInstance(
                                        value);
                                setterHelper(fieldEntry.getKey().method, rb, currency);
                            } catch (IllegalArgumentException ex) {
                                reportError.accept(String.format(
                                        "parsing currency (%s) of", value), ex);
                            }
                            break;

                        case Tags:
                            try {
                                final String[] tags = Util.splitAtWhitespace(value);
                                setterHelper(fieldEntry.getKey().method, rb, tags);
                            } catch (IllegalArgumentException ex) {
                                reportError.accept(String.format(
                                        "parsing tags (%s) of", value), ex);
                            }
                            break;

                        default:
                            try {
                                setterHelper(fieldEntry.getKey().method, rb, value);
                            } catch (RuntimeException ex) {
                                reportError.accept("reading", ex);
                            }
                    }
                }

                try {
                    customReadRecord(rb, record);

                    if (rb.getAmount().isEmpty()) {
                        // Case of "Order" records. No amount specified.
                        continue;
                    }

                    result.add(rb.create());
                } catch (RuntimeException ex) {
                    reportError.accept("reading", ex);
                }
            }
        }

        return result.stream();
    }

    protected void customReadRecord(RecordBuilder rb, CSVRecord record) {
    }

    protected abstract Map<RecordBuilder.Setter, Enum<?>> fieldMapper();

    protected abstract DateTimeFormatter recordDateTimeFormatter();

    protected abstract CSVFormat initParser();

    private static <T> void setterHelper(BiConsumer<RecordBuilder, ?> method,
            RecordBuilder rb, T value) {
        ((BiConsumer<RecordBuilder, T>) method).accept(rb, value);
    }
}
