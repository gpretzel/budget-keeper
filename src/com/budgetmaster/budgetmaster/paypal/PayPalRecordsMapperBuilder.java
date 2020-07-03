package com.budgetmaster.budgetmaster.paypal;

import com.budgetmaster.budgetmaster.MarketplaceTransactions;
import com.budgetmaster.budgetmaster.MarketplaceTransactionsBuilder;
import com.budgetmaster.budgetmaster.Record;
import com.budgetmaster.budgetmaster.RecordsReaderCsv;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import com.budgetmaster.budgetmaster.Util;
import org.w3c.dom.Element;
import com.budgetmaster.budgetmaster.RecordsMapper;


public class PayPalRecordsMapperBuilder extends MarketplaceTransactionsBuilder<Record> {

    @Override
    protected MarketplaceTransactions<Record> createMarketplaceTransactions()
            throws IOException {
        Stream<Record> payPalRecords = new RecordsReaderCsv().read(
                inputPayPalCsvFile);
        return new MarketplaceTransactions<>(payPalRecords,
                Record::getAmount, Record::getCurrency,
                Record::getTransactionDate);
    }

    @Override
    protected RecordsMapper createRecordsFilter(
            MarketplaceTransactions<Record> transactions) {
        return new PayPalRecordsMapper(transactions, unclaimedPayPalCsvFile);
    }

    @Override
    public void initFromXml(Element root) {
        super.initFromXml(root);

        String text = Util.readLastElement(root, "source-csv");
        if (text != null) {
            inputPayPalCsvFile = Path.of(text);
        }

        text = Util.readLastElement(root, "unclaimed-collector-csv");
        if (text != null) {
            unclaimedPayPalCsvFile = Path.of(text);
        }
    }

    private Path inputPayPalCsvFile;
    private Path unclaimedPayPalCsvFile;
}
