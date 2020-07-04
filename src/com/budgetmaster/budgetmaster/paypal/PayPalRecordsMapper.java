package com.budgetmaster.budgetmaster.paypal;

import com.budgetmaster.budgetmaster.Functional.ThrowingRunnable;
import com.budgetmaster.budgetmaster.LoggingRecordMapper;
import com.budgetmaster.budgetmaster.MarketplaceTransactions;
import com.budgetmaster.budgetmaster.Record;
import com.budgetmaster.budgetmaster.Util;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;
import com.budgetmaster.budgetmaster.RecordsMapper;
import java.util.stream.Collectors;


final class PayPalRecordsMapper implements RecordsMapper {
    @Override
    public Stream<Record> apply(Stream<Record> records) {
        return transactions.mapRecords(records, (match) -> {
            return transactions.mapRecord(match, Record::getDescription,
                    (record) -> Stream.of(record.getTags()).collect(
                            Collectors.toSet()), LOGGER);
        });
    }

    @Override
    public void onRecordsProcessed() {
        if (unclaimedRecordsCsvFile != null) {
            ThrowingRunnable.toRunnable(() -> Util.saveToCsvFile(
                    unclaimedRecordsCsvFile,
                    (Stream<Record>) transactions.getUnclaimedTransactions())).run();
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
