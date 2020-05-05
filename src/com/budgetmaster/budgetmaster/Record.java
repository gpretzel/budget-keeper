package com.budgetmaster.budgetmaster;

import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

final class Record {
    LocalDate getDate() {
        return date;
    }

    String getDescription() {
        return desc;
    }
    
    Currency getCurrency() {
        return currency;
    }

    String getAmount() {
        return amount;
    }
    
    String getCategory() {
        return category;
    }
    
    Record(LocalDate date, String desc, Currency currency, String amount,
            String category) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(desc);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(amount);
        
        this.date = date;
        this.desc = desc;
        this.currency = currency;
        this.amount = amount;
        this.category = category;
    }

    private final LocalDate date;
    private final String desc;
    private final String category;
    private final Currency currency;
    private final String amount;
}
