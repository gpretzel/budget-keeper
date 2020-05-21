package com.budgetmaster.budgetmaster;

import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

public final class Record {
    LocalDate getDate() {
        return date;
    }

    String getDescription() {
        return desc;
    }

    Currency getCurrency() {
        if (source != null) {
            return source.getCurrency();
        }
        return null;
    }

    String getAmount() {
        return amount;
    }

    String getCategory() {
        return category;
    }

    Statement getSource() {
        return source;
    }

    boolean isNegative() {
        return (amount.stripLeading().charAt(0) == '-');
    }

    Record(LocalDate date, String desc, String amount,
            String category, Statement source) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(desc);
        Objects.requireNonNull(amount);

        this.date = date;
        this.desc = desc;
        this.amount = amount;
        this.category = category;
        this.source = source;
    }

    private final LocalDate date;
    private final String desc;
    private final String category;
    private final String amount;
    private final Statement source;
}
