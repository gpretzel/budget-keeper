package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class RecordCsvSerializer {
    void saveToFile(Stream<Record> records, Path csvOutputFile) throws IOException {
        Files.write(csvOutputFile, records
                .filter(Objects::nonNull)
                .map(this::toCsvString)
                .collect(Collectors.toList()));
    }

    private String toCsvString(Record record) {
        String date = formatField(record.getDate().format(DEFAULT_DATE_FORMAT));
        String desc = formatField(record.getDescription());
        String currency = formatField(record.getCurrency().getCurrencyCode());
        String amount = formatField(record.getAmount());
        String category = formatField(record.getCategory());
        String statementId = formatField(record.getSource().getId());
        String statementSystemId = formatField(record.getSource().getSystemId());
    
        return String.join(",", date, desc, currency, amount, category,
                statementId, statementSystemId);
    }
    
    private String formatField(String value) {
        return String.format("\"%s\"", value);
    }

    private final static DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "dd/MM/yy", Locale.US);
}
