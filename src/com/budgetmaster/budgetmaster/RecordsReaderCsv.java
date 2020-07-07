package com.budgetmaster.budgetmaster;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


final public class RecordsReaderCsv extends AccountStatementCsv {
    @Override
    protected Map<RecordBuilder.Setter, Enum<?>> fieldMapper() {
        return Map.of(
                RecordBuilder.Setter.Amount, CsvRecordsSerializer.Field.Amount,
                RecordBuilder.Setter.Id, CsvRecordsSerializer.Field.Id,
                RecordBuilder.Setter.TransactionDate, CsvRecordsSerializer.Field.TransactionDate,
                RecordBuilder.Setter.PostingDate, CsvRecordsSerializer.Field.PostingDate,
                RecordBuilder.Setter.Currency, CsvRecordsSerializer.Field.Currency,
                RecordBuilder.Setter.Description, CsvRecordsSerializer.Field.Description,
                RecordBuilder.Setter.Tags, CsvRecordsSerializer.Field.Tags,
                RecordBuilder.Setter.Category, CsvRecordsSerializer.Field.Category
        );
    }

    @Override
    protected DateTimeFormatter recordDateTimeFormatter() {
        return new CsvRecordsSerializer().getDateTimeFormatter();
    }

    @Override
    protected CSVFormat initFormat() {
        String header[] = Stream.of(new CsvRecordsSerializer().getOrderedFields())
                .map(CsvRecordsSerializer.Field::name)
                .toArray(String[]::new);
        return CSVFormat.RFC4180.withFirstRecordAsHeader().withHeader(header);
    }

    @Override
    protected void customReadRecord(RecordBuilder rb, CSVRecord record) {
        String id = record.get(CsvRecordsSerializer.Field.StatementId);
        Path systemId = Path.of(record.get(CsvRecordsSerializer.Field.StatementSystemId));
        Statement statement = Statement.fromStatementFile(id, systemId, null, null);
        rb.setSource(statement);
    }
}
