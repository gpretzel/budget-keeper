package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.MarketplaceTransactions;
import com.budgetmaster.budgetmaster.MarketplaceTransactionsBuilder;
import com.budgetmaster.budgetmaster.Record;
import java.io.IOException;
import java.nio.file.Path;
import com.budgetmaster.budgetmaster.RecordsMapper;


public class AmazonRecordsMapperBuilder extends MarketplaceTransactionsBuilder<Record> {

    @Override
    protected MarketplaceTransactions<Record> createMarketplaceTransactions()
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected RecordsMapper createRecordsFilter(
            MarketplaceTransactions<Record> transactions) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public AmazonRecordsMapperBuilder setInputOrdersCsvFile(Path v) {
        inputOrdersCsvFile = v;
        return this;
    }

    public AmazonRecordsMapperBuilder setInputRefundsCsvFile(Path v) {
        inputRefundsCsvFile = v;
        return this;
    }

    public AmazonRecordsMapperBuilder setUnclaimedOrdersCsvFile(Path v) {
        inputOrdersCsvFile = v;
        return this;
    }

    public AmazonRecordsMapperBuilder seUnclaimedtRefundsCsvFile(Path v) {
        inputRefundsCsvFile = v;
        return this;
    }

    private Path inputOrdersCsvFile;
    private Path inputRefundsCsvFile;
}
