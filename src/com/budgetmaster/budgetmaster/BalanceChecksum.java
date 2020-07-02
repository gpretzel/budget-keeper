package com.budgetmaster.budgetmaster;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;


public final class BalanceChecksum {
    public BalanceChecksum (String amount) {
        expectedAmount = new BigDecimal(amount);
    }

    public BalanceChecksum (BigDecimal amount) {
        expectedAmount = amount;
    }

    public BalanceChecksum append(Stream<Record> records) {
        BigDecimal amount = records.sequential()
                .map(Record::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (currentAmount == null) {
            currentAmount = amount;
        } else {
            currentAmount = currentAmount.add(amount);
        }
        return this;
    }

    public void validate() {
        LOGGER.finer(String.format("Check total harvested records amount is [%s]",
                expectedAmount));
        if (expectedAmount.compareTo(currentAmount) != 0) {
            throw new RuntimeException(String.format(
                    "Expected amount: [%s]; actual: [%s]", expectedAmount,
                    currentAmount));
        }
    }

    private final BigDecimal expectedAmount;
    private BigDecimal currentAmount;

    private static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());
}
