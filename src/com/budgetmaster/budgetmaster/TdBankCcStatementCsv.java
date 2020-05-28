package com.budgetmaster.budgetmaster;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;


final class TdBankCcStatementCsv extends AccountStatementCsv {
    @Override
    protected Map<RecordBuilder.Setter, Enum<?>> fieldMapper() {
        return Map.of(
                RecordBuilder.Setter.Amount, Headers.Amount,
                RecordBuilder.Setter.TransactionDate, Headers.Date,
                RecordBuilder.Setter.Description, Headers.MerchantName,
                RecordBuilder.Setter.Category, Headers.MerchantCategoryDescription
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
        ActivityType,
        MerchantName,
        MerchantCategoryDescription,
        Amount,
        Rewards
    };

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd");
}
