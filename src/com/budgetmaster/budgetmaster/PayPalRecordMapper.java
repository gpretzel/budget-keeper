package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class PayPalRecordMapper implements UnaryOperator<Record> {
    static PayPalRecordMapper createFromCvsFile(Path path) throws IOException {
        Stream<Record> records = new RecordsReaderCsv().read(path);
        return new PayPalRecordMapper(records);
    }

    @Override
    public Record apply(Record r) {
        final String key = createKey(r);
        Record mathcedRecords[] = amountMap.get(key);
        if (mathcedRecords != null) {
            Record matched = mathcedRecords[0];
                    
            RecordBuilder rb = RecordBuilder.from(r);
            rb.setDescription(matched.getDescription());
            
            LoggingRecordMapper.LOGGER.info(String.format(
                    "Set description to [%s] of [%s]", rb.getDescription(), r));
            return rb.create();
        }
        return r;
    }

    private static String createKey(Record record) {
        return String.format("%s%s", record.getAmount(), record.getCurrencyCode());
    }
    
    private PayPalRecordMapper(Stream<Record> allRecords) {
        amountMap = allRecords.collect(Collectors.toMap(
                PayPalRecordMapper::createKey,
                record -> new Record[] { record }, 
                (a, b) -> Stream.concat(Stream.of(a), Stream.of(b)).toArray(Record[]::new)));
    }
    
    private final Map<String, Record[]> amountMap;
}
