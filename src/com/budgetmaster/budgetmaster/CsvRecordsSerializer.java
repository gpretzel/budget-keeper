package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;


final class CsvRecordsSerializer {
    CsvRecordsSerializer() {
        // Date format allowing proper sorting of dates as strings.
        setDateFormat("yyyy/MM/dd");
        setRecordFormat(Field.values());
    }

    void saveToFile(Stream<Record> records, Path out) throws IOException {
        try (var appendable = Files.newBufferedWriter(out)) {
            save(records, appendable);
        }
    }

    void saveToStream(Stream<Record> records, Appendable appendable) throws
            IOException {
        save(records, appendable);
    }

    DateTimeFormatter getDateTimeFormatter() {
        return dateFormat;
    }

    Field[] getOrderedFields() {
        if (recordFormatMap == null) {
            throw new IllegalStateException();
        }

        return recordFormatMap.entrySet().stream()
                .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(e -> e.getKey())
                .toArray(Field[]::new);
    }

    CsvRecordsSerializer setDateFormat(String formatStr, String localeStr) {
        final Locale locale;
        if (localeStr == null) {
            locale = Locale.US;
        } else {
            locale = Locale.forLanguageTag(localeStr);
        }
        dateFormat = DateTimeFormatter.ofPattern(formatStr, locale);
        return this;
    }

    CsvRecordsSerializer setDateFormat(String formatStr) {
        return setDateFormat(formatStr, null);
    }

    CsvRecordsSerializer setRecordFormat(Field[] fields) {
        recordFormatMap = Stream.of(fields).collect(Collectors.toMap(
                field -> field, field -> field.ordinal()));
        return this;
    }

    private void save(Stream<Record> records, Appendable appendable) throws
            IOException {
        CSVFormat format = CSVFormat.RFC4180;
        format = format.withHeader(
                Stream.of(getOrderedFields()).map(Field::name).toArray(
                        String[]::new));
        try (CSVPrinter csvPrinter = new CSVPrinter(appendable, format)) {
            records.map(this::toStringArray).forEachOrdered(
                    ThrowingConsumer.toConsumer(csvPrinter::printRecord));
        } catch (Functional.ExceptionBox ex) {
            var cause = ex.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw ex;
        }
    }

    private void setFieldValue(String[] recordFields, Field field,
            Supplier<String> valueSupplier) {
        if (recordFormatMap.containsKey(field)) {
            String value = valueSupplier.get();
            if (value != null) {
                value = String.format("%s", value);
            } else {
                value = "";
            }
            recordFields[recordFormatMap.get(field)] = value;
        }
    }

    private String[] toStringArray(Record record) {
        final String[] recordFields = new String[recordFormatMap.size()];

        setFieldValue(recordFields, Field.TransactionDate,
                () -> record.getTransactionDate().format(dateFormat));
        setFieldValue(recordFields, Field.PostingDate,
                () -> record.getPostingDate().format(dateFormat));
        setFieldValue(recordFields, Field.Id, () -> record.getId());
        setFieldValue(recordFields, Field.Amount, () -> record.getAmount().toPlainString());
        setFieldValue(recordFields, Field.Category, () -> record.getCategory());
        setFieldValue(recordFields, Field.Currency, () -> record.getCurrencyCode());
        setFieldValue(recordFields, Field.Description,
                () -> record.getDescription());
        setFieldValue(recordFields, Field.StatementId, () -> record.getSource()
                == null ? null : record.getSource().getId());
        setFieldValue(recordFields, Field.StatementSystemId,
                () -> record.getSource() == null ? null : record.getSource().getSystemId());
        setFieldValue(recordFields, Field.Tags,
                () -> record.getTags() == null ? null : String.join(" ",
                record.getTags()));
        
        return recordFields;
    }

    enum Field {
        TransactionDate("TDATE"),
        PostingDate("PDATE"),
        Amount,
        Currency,
        Category,
        Tags,
        Description("DESC"),        
        StatementId("SCLASS"),
        StatementSystemId("SID"),
        Id;

        Field() {
            tag = name().toUpperCase();
        }

        Field(String tag) {
            this.tag = tag;
        }

        final String tag;
    };

    private DateTimeFormatter dateFormat;
    private Map<Field, Integer> recordFormatMap;
}
