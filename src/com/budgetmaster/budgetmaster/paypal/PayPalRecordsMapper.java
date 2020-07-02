package com.budgetmaster.budgetmaster.paypal;

import com.budgetmaster.budgetmaster.Functional.ThrowingRunnable;
import com.budgetmaster.budgetmaster.LoggingRecordMapper;
import com.budgetmaster.budgetmaster.MarketplaceTransactions;
import com.budgetmaster.budgetmaster.Record;
import com.budgetmaster.budgetmaster.RecordBuilder;
import com.budgetmaster.budgetmaster.Util;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import com.budgetmaster.budgetmaster.RecordsMapper;


final class PayPalRecordsMapper implements RecordsMapper {
    @Override
    public Stream<Record> apply(Stream<Record> records) {
        return transactions.filterRecords(records, (match) -> {
            RecordBuilder rb = RecordBuilder.from(match.getKey());
            rb.setDescription(match.getValue().getDescription());

            StringBuilder logMsg = null;
            if (LOGGER.isLoggable(Level.INFO)) {
                logMsg = new StringBuilder();
                logMsg.append(String.format("Set [%s] description",
                        rb.getDescription()));
            }

            if (match.getValue().getTags() != null) {
                Set<String> oldTags = null;
                if (LOGGER.isLoggable(Level.INFO)) {
                    if (rb.getTags() != null) {
                        oldTags = Set.of(rb.getTags());
                    } else {
                        oldTags = Set.of();
                    }
                }

                Set.of(match.getValue().getTags()).forEach(rb::addTag);

                if (LOGGER.isLoggable(Level.INFO)) {
                    Set<String> addedTags = new HashSet<>(Set.of(rb.getTags()));
                    addedTags.removeAll(oldTags);

                    if (!addedTags.isEmpty()) {
                        logMsg.append(String.format("; add %s tags", addedTags));
                    }
                }
            }

            LOGGER.info(String.format("%s in [%s]", logMsg, match));

            return rb.create();
        });
    }

    @Override
    public void onRecordsProcessed() {
        if (unclaimedRecordsCsvFile != null) {
            ThrowingRunnable.toRunnable(() -> Util.saveToCsvFile(
                    unclaimedRecordsCsvFile,
                    transactions.getUnclaimedTransactions())).run();
        }
    }

    PayPalRecordsMapper(MarketplaceTransactions<Record> transactions,
            Path unclaimedRecordsCsvFile) {
        this.transactions = transactions;
        this.unclaimedRecordsCsvFile = unclaimedRecordsCsvFile;
    }

    private final MarketplaceTransactions<Record> transactions;
    private final Path unclaimedRecordsCsvFile;

    private final static Logger LOGGER = LoggingRecordMapper.LOGGER;
}
