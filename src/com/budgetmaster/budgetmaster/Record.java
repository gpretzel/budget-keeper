package com.budgetmaster.budgetmaster;

import java.nio.file.Path;
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
        return currency;
    }
    
    String getCurrencyCode() {
        if (currency == null) {
            return null;
        }
        return currency.getCurrencyCode();
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
            String category, Currency currency, Statement source) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(desc);
        Objects.requireNonNull(amount);

        this.date = date;
        this.desc = desc;
        this.amount = amount;
        this.category = category;
        this.source = source;
        this.currency = currency;
    }
    
    @Override
    public String toString() {
        if (source == null) {
            return String.format("%s|%s|%s|%s", date, amount,
                    Util.appendEllipsis(desc, 40), Util.appendEllipsis(category,
                    20));
        }

        return String.format("%s|%s|%s|%s|%s|%s|%s", date, amount,
                Util.appendEllipsis(desc, 40), Util.appendEllipsis(category, 20),
                getCurrency(), source.getId(), Util.pathEllipsis(Path.of(
                source.getSystemId()), 40));
    }

    private final LocalDate date;
    private final String desc;
    private final String category;
    private final String amount;
    private final Statement source;
    private final Currency currency;
}
