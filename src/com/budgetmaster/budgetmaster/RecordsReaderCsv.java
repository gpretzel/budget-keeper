package com.budgetmaster.budgetmaster;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


final class RecordsReaderCsv extends AccountStatementCsv {
    @Override
    protected Map<RecordBuilder.Setter, Enum<?>> fieldMapper() {
        return Map.of(
                RecordBuilder.Setter.Amount, RecordsSerializer.Field.Amount,
                RecordBuilder.Setter.Id, RecordsSerializer.Field.Id,
                RecordBuilder.Setter.TransactionDate, RecordsSerializer.Field.TransactionDate,
                RecordBuilder.Setter.PostingDate, RecordsSerializer.Field.PostingDate,
                RecordBuilder.Setter.Currency, RecordsSerializer.Field.Currency,
                RecordBuilder.Setter.Description, RecordsSerializer.Field.Description,
                RecordBuilder.Setter.Tags, RecordsSerializer.Field.Tags,
                RecordBuilder.Setter.Category, RecordsSerializer.Field.Category
        );
    }

    @Override
    protected DateTimeFormatter recordDateTimeFormatter() {
        return RecordsSerializer.csv().getDateTimeFormatter();
    }

    @Override
    protected CSVFormat initParser() {
        String header[] = Stream.of(RecordsSerializer.csv().getOrderedFields())
                .map(RecordsSerializer.Field::name)
                .toArray(String[]::new);
        return CSVFormat.RFC4180.withFirstRecordAsHeader().withHeader(header);
    }

    @Override
    protected void customReadRecord(RecordBuilder rb, CSVRecord record) {
        String id = record.get(RecordsSerializer.Field.StatementId);
        Path systemId = Path.of(record.get(RecordsSerializer.Field.StatementSystemId));
        Statement statement = Statement.fromStatementFile(id, systemId, null, null);
        rb.setSource(statement);
    }
}
