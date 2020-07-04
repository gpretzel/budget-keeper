package com.budgetmaster.budgetmaster.paypal;

import com.budgetmaster.budgetmaster.Functional.ThrowingSupplier;
import com.budgetmaster.budgetmaster.MarketplaceTransactions;
import com.budgetmaster.budgetmaster.MarketplaceTransactionsInitializer;
import com.budgetmaster.budgetmaster.PluggableSupplier;
import com.budgetmaster.budgetmaster.Record;
import com.budgetmaster.budgetmaster.RecordsReaderCsv;
import java.nio.file.Path;
import java.util.stream.Stream;
import com.budgetmaster.budgetmaster.Util;
import org.w3c.dom.Element;
import com.budgetmaster.budgetmaster.RecordsMapper;


public class PayPalRecordsMapperBuilder implements PluggableSupplier<RecordsMapper> {

    @Override
    public RecordsMapper get() {
        Stream<Record> payPalRecords = ThrowingSupplier.toSupplier(
                () -> new RecordsReaderCsv().read(inputPayPalCsvFile)).get();

        var mt = new MarketplaceTransactions<>(payPalRecords, Record::getAmount,
                Record::getCurrency, Record::getTransactionDate);

        mti.accept(mt);

        return new PayPalRecordsMapper(mt, unclaimedPayPalCsvFile);
    }

    @Override
    public void initFromXml(Element root) {
        mti.initFromXml(root);

        inputPayPalCsvFile = Util.readLastElementAsPath(root, "source-csv");

        unclaimedPayPalCsvFile = Util.readLastElementAsPath(root,
                "unclaimed-collector-csv");
    }

    private Path inputPayPalCsvFile;
    private Path unclaimedPayPalCsvFile;
    private final MarketplaceTransactionsInitializer mti = new MarketplaceTransactionsInitializer();
}
