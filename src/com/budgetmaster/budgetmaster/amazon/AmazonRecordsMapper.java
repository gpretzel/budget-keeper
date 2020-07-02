package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.LoggingRecordMapper;
import com.budgetmaster.budgetmaster.Record;
import java.util.logging.Logger;
import java.util.stream.Stream;
import com.budgetmaster.budgetmaster.RecordsMapper;


final class AmazonRecordsMapper implements RecordsMapper {
    AmazonRecordsMapper(Stream<AmazonOrder> orders, Stream<AmazonRefund> refunds) {
    }

    @Override
    public void onRecordsProcessed() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Stream<Record> apply(Stream<Record> t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private final static Logger LOGGER = LoggingRecordMapper.LOGGER;
}
