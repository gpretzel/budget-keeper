package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingSupplier;
import java.io.IOException;
import org.w3c.dom.Element;


public abstract class MarketplaceTransactionsBuilder<T> implements PluggableSupplier<RecordsMapper> {

    protected abstract MarketplaceTransactions<T> createMarketplaceTransactions()
            throws IOException;

    protected abstract RecordsMapper createRecordsFilter(
            MarketplaceTransactions<T> transactions);

    @Override
    public RecordsMapper get() {
        MarketplaceTransactions<T> result = ThrowingSupplier.toSupplier(
                () -> createMarketplaceTransactions()).get();
        result.setMaxDaysDiff(maxDaysDiff);
        result.setMaxAmountDiff(maxAmountDiff);
        return createRecordsFilter(result);
    }

    @Override
    public void initFromXml(Element root) {
        String text = Util.readLastElement(root, "date-diff-days");
        if (text != null) {
            maxDaysDiff = Long.parseLong(text);
        }

        text = Util.readLastElement(root, "amount-diff");
        if (text != null) {
            maxAmountDiff = Double.parseDouble(text);
        }
    }

    private long maxDaysDiff;
    private double maxAmountDiff;
}
