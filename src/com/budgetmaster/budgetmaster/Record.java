package com.budgetmaster.budgetmaster;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import java.util.Set;

public final class Record {
    LocalDate getTransactionDate() {
        return transactionDate;
    }

    LocalDate getPostingDate() {
        return postingDate;
    }

    String getDescription() {
        return desc;
    }

    String getId() {
        return id;
    }

    String[] getTags() {
        return tags;
    }

    boolean hasTag(String tag) {
        Objects.requireNonNull(tag);
        if (tags == null) {
            return false;
        }
        return Set.of(tags).contains(tag);
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

    Record(LocalDate transactionDate, LocalDate postingDate, String desc,
            String amount, String category, Currency currency, Statement source,
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
        sb.append(String.format("|%s|%s", amount, Util.appendEllipsis(desc, 40)));
        if (category != null) {
            sb.append('|');
            sb.append(Util.appendEllipsis(category, 20));
        }

        if (tags != null) {
            sb.append(String.format("|%s", (Object)tags));
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
    private final String amount;
    private final Statement source;
    private final Currency currency;
    private final String id;
    private final String[] tags;
}
