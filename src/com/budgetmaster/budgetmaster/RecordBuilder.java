package com.budgetmaster.budgetmaster;

import java.time.LocalDate;
import java.util.Currency;
import java.util.function.BiConsumer;

public final class RecordBuilder {
    Record create() {
        return new Record(date, filterSting(desc), filterAmount(), filterSting(
                category), currency, source);
    }

    static RecordBuilder from(Record record) {
        return new RecordBuilder()
                .setAmount(record.getAmount())
                .setDescription(record.getDescription())
                .setDate(record.getDate())
                .setCategory(record.getCategory())
                .setCurrency(record.getCurrency())
                .setSource(record.getSource());
    }

    LocalDate getDate() {
        return date;
    }

    RecordBuilder setDate(LocalDate v) {
        date = v;
        return this;
    }

    String getDescription() {
        return desc;
    }

    RecordBuilder setDescription(String v) {
        desc = v;
        return this;
    }

    Currency getCurrency() {
        return currency;
    }

    RecordBuilder setCurrency(Currency v) {
        currency = v;
        return this;
    }

    String getCategory() {
        return category;
    }

    RecordBuilder setCategory(String v) {
        category = v;
        return this;
    }

    Statement getSource() {
        return source;
    }

    RecordBuilder setSource(Statement v) {
        source = v;
        return this;
    }

    String getAmount() {
        return amount;
    }

    RecordBuilder setAmount(String v) {
        amount = v;
        return this;
    }

    RecordBuilder negateAmount(boolean v) {
        negateAmount = v;
        return this;
    }

    RecordBuilder negateAmount() {
        return negateAmount(true);
    }

    RecordBuilder strip(boolean v) {
        strip = v;
        return this;
    }

    RecordBuilder strip() {
        return strip(true);
    }

    private String filterSting(String v) {
        if (strip && v != null) {
            return v.strip();
        }
        return v;
    }

    private String filterAmount() {
        return MonetaryAmountNormalizer.negate(negateAmount).normalize(amount);
    }

    private boolean negateAmount;
    private boolean strip;
    private LocalDate date;
    private String desc;
    private String category;
    private Statement source;
    private String amount;
    private Currency currency;
    
    public enum Setter {
        Amount((rb, v) -> rb.setAmount((String)v)),
        Description((rb, v) -> rb.setDescription((String)v)),
        Currency((rb, v) -> rb.setCurrency((Currency)v)),
        Category((rb, v) -> rb.setCategory((String)v)),
        Source((rb, v) -> rb.setSource((Statement)v)),
        Date((rb, v) -> rb.setDate((LocalDate)v));

        Setter(BiConsumer<RecordBuilder, ?> method) {
            this.method = method;
        }
        
        final BiConsumer<RecordBuilder, ?> method;
    };
}
