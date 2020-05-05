package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


final class TdBankCcStatementCsv implements RecordSupplier {
    @Override
    public List<Record> read(Path csvFilePath) throws IOException {
        List<Record> result = new ArrayList<>();
        try (Reader in = Files.newBufferedReader(csvFilePath)) {
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().withHeader(
                    Headers.class).parse(in);
            for (CSVRecord record : records) {
                final String dateStr = record.get(Headers.Date);                
                try {
                    final LocalDate date = LocalDate.parse(dateStr,
                            DATE_FORMATTER);

                    result.add(new RecordBuilder()
                            .setDate(date)
                            .setAmount(record.get(Headers.Amount))
                            .setDescription(record.get(Headers.MerchantName))
                            .setCategory(record.get(Headers.MerchantCategoryDescription))
                            .create());
                } catch (DateTimeParseException ex) {
                    
                }
            }
        }
        
        return result;
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
