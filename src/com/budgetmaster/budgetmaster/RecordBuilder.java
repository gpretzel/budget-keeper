package com.budgetmaster.budgetmaster;

import java.time.LocalDate;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class RecordBuilder {
    Record create() {
        return new Record(transactionDate, postingDate, filterSting(desc),
                filterAmount(), filterSting(category), currency, source,
                filterSting(id), tags);
    }

    static RecordBuilder from(Record record) {
        return new RecordBuilder()
                .setAmount(record.getAmount())
                .setDescription(record.getDescription())
                .setTransactionDate(record.getTransactionDate())
                .setPostingDate(record.getPostingDate())
                .setCategory(record.getCategory())
                .setCurrency(record.getCurrency())
                .setId(record.getId())
                .setTags(record.getTags())
                .setSource(record.getSource());
    }

    LocalDate getTransactionDate() {
        return transactionDate;
    }

    RecordBuilder setTransactionDate(LocalDate v) {
        transactionDate = v;
        return this;
    }

    LocalDate getPostingDate() {
        return postingDate;
    }

    RecordBuilder setPostingDate(LocalDate v) {
        postingDate = v;
        return this;
    }

    String getDescription() {
        return desc;
    }

    RecordBuilder setDescription(String v) {
        desc = v;
        return this;
    }

    String[] getTags() {
        return tags;
    }

    RecordBuilder setTags(String[] v) {
        tags = v;
        validateTags();
        return this;
    }

    RecordBuilder addTag(String v) {
        validateTag(v);
        Set<String> newTags = new HashSet<>();
        if (tags != null) {
            newTags.addAll(List.of(tags));
        }
        newTags.add(v);
        tags = newTags.toArray(String[]::new);
        return this;
    }

    String getId() {
        return id;
    }

    RecordBuilder setId(String v) {
        id = v;
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

    private static void validateTag(String v) {
        Objects.requireNonNull(v);
        if (!VALIDATE_TAG_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid tag value [%s]", v));
        }
    }

    private void validateTags() {
        if (tags != null) {
            Stream.of(tags).forEach(RecordBuilder::validateTag);
        }
    }

    private boolean negateAmount;
    private boolean strip;
    private LocalDate transactionDate;
    private LocalDate postingDate;
    private String desc;
    private String category;
    private Statement source;
    private String amount;
    private Currency currency;
    private String id;
    private String[] tags;

    public enum Setter {
        Amount((rb, v) -> rb.setAmount((String)v)),
        Description((rb, v) -> rb.setDescription((String)v)),
        Currency((rb, v) -> rb.setCurrency((Currency)v)),
        Category((rb, v) -> rb.setCategory((String)v)),
        Source((rb, v) -> rb.setSource((Statement)v)),
        Id((rb, v) -> rb.setId((String)v)),
        Tags((rb, v) -> rb.setTags((String[])v)),
        PostingDate((rb, v) -> rb.setPostingDate((LocalDate)v)),
        TransactionDate((rb, v) -> rb.setTransactionDate((LocalDate)v));

        Setter(BiConsumer<RecordBuilder, ?> method) {
            this.method = method;
        }

        final BiConsumer<RecordBuilder, ?> method;
    };

    private final static Pattern VALIDATE_TAG_PATTERN = Pattern.compile("^\\w+$");
}
