package com.budgetmaster.budgetmaster;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Currency;
import java.util.Objects;
import java.util.Set;

public final class Record {
    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public String getDescription() {
        return desc;
    }

    public String getId() {
        return id;
    }
    
    public String getGlobalId() {
        return String.format("%s-%s", source.getSystemId(), getId());
    }

    public String[] getTags() {
        return tags;
    }

    public boolean hasTag(String tag) {
        Objects.requireNonNull(tag);
        if (tags == null) {
            return false;
        }
        return Set.of(tags).contains(tag);
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        if (currency == null) {
            return null;
        }
        return currency.getCurrencyCode();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public Statement getSource() {
        return source;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    Record(LocalDate transactionDate, LocalDate postingDate, String desc,
            BigDecimal amount, String category, Currency currency, Statement source,
            String id, String[] tags) {
        Objects.requireNonNull(transactionDate);
        Objects.requireNonNull(desc);
        Objects.requireNonNull(amount);

        this.transactionDate = transactionDate;
        if (postingDate != null) {
            this.postingDate = postingDate;
        } else {
            this.postingDate = transactionDate;
        }
        this.desc = desc;
        this.amount = amount;
        this.category = category;
        this.source = source;
        this.currency = currency;
        this.id = id;
        this.tags = tags;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (transactionDate.isEqual(postingDate)) {
            sb.append(String.format("%s", transactionDate));
        } else {
            sb.append(String.format("%s|%s", transactionDate, postingDate));
        }
        sb.append(String.format("|%s|%s", amount.toPlainString(), Util.appendEllipsis(desc, 40)));
        if (category != null) {
            sb.append('|');
            sb.append(Util.appendEllipsis(category, 20));
        }

        if (tags != null) {
            sb.append(String.format("|%s", Arrays.toString(tags)));
        }

        if (currency != null) {
            sb.append(String.format("|%s", getCurrencyCode()));
        }

        if (id != null) {
            sb.append('|');
            sb.append(Util.appendEllipsis(id, 10));
        }

        if (source != null) {
            sb.append(String.format("|%s|%s", source.getId(), Util.pathEllipsis(
                    Path.of(source.getSystemId()), 40)));
        }

        return sb.toString();
    }

    private final LocalDate transactionDate;
    private final LocalDate postingDate;
    private final String desc;
    private final String category;
    private final BigDecimal amount;
    private final Statement source;
    private final Currency currency;
    private final String id;
    private final String[] tags;
}
