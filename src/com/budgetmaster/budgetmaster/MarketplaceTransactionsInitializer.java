package com.budgetmaster.budgetmaster;

import java.util.function.Consumer;
import org.w3c.dom.Element;

public class MarketplaceTransactionsInitializer implements Pluggable, Consumer<MarketplaceTransactions<?>> {

    @Override
    public void accept(MarketplaceTransactions<?> mt) {
        mt.setMaxDaysDiff(maxDaysDiff);
        mt.setMaxAmountDiff(maxAmountDiff);
        mt.setAmountDiffTag(amountDiffTag);
        mt.setDaysDiffTag(daysDiffTag);
    }

    @Override
    public void initFromXml(Element root) {
        String text = Util.readLastElement(root, "diff-days");
        if (text != null) {
            maxDaysDiff = Long.parseLong(text);
        }

        text = Util.readLastElement(root, "diff-amount");
        if (text != null) {
            maxAmountDiff = Double.parseDouble(text);
        }

        amountDiffTag = Util.readLastElement(root, "diff-tag-amount");

        daysDiffTag = Util.readLastElement(root, "diff-tag-days");
    }

    private String amountDiffTag;
    private String daysDiffTag;
    private long maxDaysDiff;
    private double maxAmountDiff;
}
