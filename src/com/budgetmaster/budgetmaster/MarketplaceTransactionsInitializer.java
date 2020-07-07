package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.MarketplaceTransactions.IntRange;
import static com.budgetmaster.budgetmaster.Util.queryNodes;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MarketplaceTransactionsInitializer implements Pluggable, Consumer<MarketplaceTransactions<?>> {

    @Override
    public void accept(MarketplaceTransactions<?> mt) {
        mt.setMaxDaysRange(maxDaysRange);
        mt.setMaxAmountRange(maxAmountRange);
        mt.setBalanceRange(balanceRange);
        mt.setAmountDiffTag(amountDiffTag);
        mt.setDaysDiffTag(daysDiffTag);
    }

    @Override
    public void initFromXml(Element root) {
        maxDaysRange = initRange(root, "diff-days", 0);
        maxAmountRange = initRange(root, "diff-amount",
                MarketplaceTransactions.MAX_AMOUNT_DIFF);
        balanceRange = initRange(root, "balance",
                MarketplaceTransactions.MAX_BALANCE);

        amountDiffTag = Util.readLastElement(root, "diff-tag-amount");

        daysDiffTag = Util.readLastElement(root, "diff-tag-days");
    }
    
    private IntRange initRange(Element el, String elementName, int maxValue) {
        NodeList nodes = queryNodes(elementName + "[last()]", el);
        if (nodes.getLength() != 0) {
            return initRange((Element)nodes.item(0), maxValue);
        }
        return null;
    }

    private IntRange initRange(Element el, int maxValue) {
        int from = 0;
        int to = 0;
        int step = 1;
        
        Function<String, Integer> parseValue = v -> {
            if (maxValue == 0) {
                return Integer.parseInt(v);
            }
            return new BigDecimal(v).multiply(new BigDecimal(maxValue),
                    MarketplaceTransactions.MC).intValue();
        };

        if (!el.hasAttributes()) {
            int value = parseValue.apply(el.getFirstChild().getNodeValue());
            return new IntRange(value, value, step);
        }

        if (el.hasAttribute("from")) {
            from = parseValue.apply(el.getAttribute("from"));
        }

        if (el.hasAttribute("to")) {
            to = parseValue.apply(el.getAttribute("to"));
        }

        if (el.hasAttribute("step")) {
            step = parseValue.apply(el.getAttribute("step"));
        } else {
            step = (to - from) / DEFAULT_MAX_STEPS;
            if (step == 0) {
                step = to > from ? 1 : -1;
            }
        }

        return new IntRange(from, to, step);
    }

    private String amountDiffTag;
    private String daysDiffTag;
    private IntRange maxDaysRange;
    private IntRange maxAmountRange;
    private IntRange balanceRange;
    
    private final int DEFAULT_MAX_STEPS = 20;
}
