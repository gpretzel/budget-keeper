package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        if (mathcedRecords == null) {
            return r;
        }

        Record matched = mathcedRecords[0];

        RecordBuilder rb = RecordBuilder.from(r);
        rb.setDescription(matched.getDescription());

        StringBuilder logMsg = null;
        if (LOGGER.isLoggable(Level.INFO)) {
            logMsg = new StringBuilder();
            logMsg.append(String.format("Set [%s] description",
                    rb.getDescription()));
        }

        if (matched.getTags() != null) {
            Set<String> oldTags = null;
            if (LOGGER.isLoggable(Level.INFO)) {
                if (rb.getTags() != null) {
                    oldTags = Set.of(rb.getTags());
                } else {
                    oldTags = Set.of();
                }
            }

            Set.of(matched.getTags()).forEach(rb::addTag);

            if (LOGGER.isLoggable(Level.INFO)) {
                Set<String> addedTags = new HashSet<>(Set.of(rb.getTags()));
                addedTags.removeAll(oldTags);

                if (!addedTags.isEmpty()) {
                    logMsg.append(String.format("; add %s tags", addedTags));
                }
            }
        }

        LOGGER.info(String.format("%s to [%s]", logMsg, r));

        return rb.create();
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

    private final static Logger LOGGER = LoggingRecordMapper.LOGGER;
}
