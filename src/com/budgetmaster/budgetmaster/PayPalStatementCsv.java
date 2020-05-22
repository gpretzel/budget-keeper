package com.budgetmaster.budgetmaster;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;


final class PayPalStatementCsv extends AccountStatementCsv {
    @Override
    protected Map<RecordBuilder.Setter, Enum<?>> fieldMapper() {
        return Map.of(
                RecordBuilder.Setter.Amount, Headers.Amount, 
                RecordBuilder.Setter.Date, Headers.Date,
                RecordBuilder.Setter.Currency, Headers.Currency,
                RecordBuilder.Setter.Description, Headers.Name,
                RecordBuilder.Setter.Category, Headers.Type
        );
    }
    
    @Override
    protected DateTimeFormatter recordDateTimeFormatter() {
        return DATE_FORMATTER;
    }

    @Override
    protected CSVFormat initParser() {
        return CSVFormat.RFC4180.withFirstRecordAsHeader().withHeader(Headers.class);
    }
    
    private enum Headers {
        Date,
        Time,
        TimeZone,
        Name,
        Type,
        Status,
        Currency,
        Amount
    };

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "MM/dd/yyyy");
}
