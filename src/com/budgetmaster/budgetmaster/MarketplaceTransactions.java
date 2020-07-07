package com.budgetmaster.budgetmaster;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class MarketplaceTransactions<T> {
    public final static int MAX_MATCH_SCORE = 1000;
    public final static int MAX_BALANCE = 1000;
    public final static int MAX_AMOUNT_DIFF = 1000;

    public final static class IntRange implements Iterable<Integer> {
        IntRange(int startInclusive, int endInclusive, int step) {
            this.startInclusive = startInclusive;
            this.endInclusive = endInclusive;
            this.step = step;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return !started || value != endInclusive;
                }

                @Override
                public Integer next() {
                    started = true;
                    try {
                        return value;
                    } finally {
                        value += step;
                        if (value > endInclusive) {
                            value = endInclusive;
                        }
                    }
                }

                private int value = startInclusive;
                private boolean started;
            };
        }

        private final int startInclusive, endInclusive, step;
    }

    public final class Match {
        Match(int score, Record key, T value) {
            if (score <= 0) {
                throw new IllegalArgumentException(String.format(
                        "Invalid match score=%d for key=%s and value=%s", score,
                        key, value));
            }

            this.key = key;
            this.value = value;
            this.score = score;
        }

        public Record getKey() {
            return key;
        }

        public T getValue() {
            return value;
        }

        public int getMatchScore() {
            return score;
        }

        public BigDecimal getAmountDiff() {
            return getAmount(key).subtract(getAmount(value));
        }

        public long getDayDiff() {
            return ChronoUnit.DAYS.between(getDate(key), getDate(value));
        }

        public Set<String> getTags() {
            Set<String> tags = null;
            if (amountDiffTag != null) {
                final BigDecimal amountDiff = getAmountDiff();
                if (amountDiff.compareTo(BigDecimal.ZERO) != 0) {
                    if (tags == null) {
                        tags = new HashSet<>();
                    }
                    tags.add(amountDiffTag);
                    tags.add(String.format("%s_%s", amountDiffTag,
                            amountDiff.toPlainString()));
                }
            }

            if (daysDiffTag != null) {
                final long daysDiff = getDayDiff();
                if (daysDiff != 0) {
                    if (tags == null) {
                        tags = new HashSet<>();
                    }
                    tags.add(daysDiffTag);
                    tags.add(String.format("%s_%d", daysDiffTag, daysDiff));
                }
            }

            return tags;
        }

        @Override
        public String toString() {
            final BigDecimal amountDiff = getAmountDiff();
            final long daysDiff = getDayDiff();

            StringBuilder sb = new StringBuilder();
            if (amountDiff.compareTo(BigDecimal.ZERO) != 0) {
                sb.append(String.format(" amount-diff=%s;", amountDiff));
            }
            if (daysDiff != 0) {
                sb.append(String.format(" days-diff=%s;", daysDiff));
            }
            return String.format("score=%d;%s key=[%s]; value=[%s]", score, sb, key,
                    value);
        }

        private final Record key;
        private final T value;
        private final int score;
    };

    public MarketplaceTransactions(Stream<? extends T> transactions,
            Function<T, BigDecimal> getAmount, Function<T, Currency> getCurrency,
            Function<T, LocalDate> getDate) {
        this.getAmount = getAmount;
        this.getDate = getDate;
        this.transactionsMap = new HashMap<>();
        this.orderedTransactions = transactions.collect(Collectors.toCollection(
                LinkedHashSet::new));

        orderedTransactions.forEach(t -> {
            Currency currency = getCurrency.apply(t);
            Transactions value = transactionsMap.get(currency);
            if (value == null) {
                value = new Transactions();
                transactionsMap.put(currency, value);
            }
            value.amountSorted.add(t);
            value.dateSorted.add(t);
        });

        transactionsMap.values().forEach(v -> {
            v.amountSorted.sort((x, y) -> getAmount.apply(x).compareTo(
                    getAmount.apply(y)));
            v.dateSorted.sort((x, y) -> getDate.apply(x).compareTo(
                    getDate.apply(y)));
        });

        setMaxDaysRange(null);
        setMaxAmountRange(null);
        setBalanceRange(null);
    }

    public MarketplaceTransactions<T> setMaxDaysRange(IntRange v) {
        if (v == null) {
            maxDaysRange = new IntRange(0, 0, 0);
        } else {
            maxDaysRange = v;
        }
        return this;
    }

    public MarketplaceTransactions<T> setMaxAmountRange(IntRange v) {
        if (v == null) {
            maxAmountRange = new IntRange(0, 0, 0);
        } else {
            maxAmountRange = v;
        }
        return this;
    }

    public MarketplaceTransactions<T> setBalanceRange(IntRange v) {
        if (v == null) {
            balanceRange = new IntRange(MAX_BALANCE / 2, MAX_BALANCE / 2, 0);
        } else {
            balanceRange = v;
        }
        return this;
    }

    public MarketplaceTransactions<T> setAmountDiffTag(String v) {
        amountDiffTag = v;
        return this;
    }

    public MarketplaceTransactions<T> setDaysDiffTag(String v) {
        daysDiffTag = v;
        return this;
    }

    public MarketplaceTransactions<T> setRecordFilter(Predicate<Record> v) {
        recordFilter = v;
        return this;
    }

    public Stream<Record> mapRecords(Stream<Record> records,
            Function<Match, Record> merger) {

        final long transactionCount = orderedTransactions.size();

        List<Record> srcRecords = records.collect(Collectors.toList());
        Map<Record, MatchInternal> matches = null;
        long matchesCount = 0;
        Iteration bestIteration = null;
        for (int maxDays : maxDaysRange) {
            for (int maxAmount : maxAmountRange) {
                for (int balance : balanceRange) {
                    iteration = new Iteration(maxDays, maxAmount, balance);
                    Map<Record, MatchInternal> newMatches = iteration.mapRecords(
                            srcRecords.stream());
                    long newMatchesCount = newMatches.values().stream().filter(
                            Objects::nonNull).count();

                    LOGGER.finer(String.format("%s; unmatched=%d", iteration,
                            srcRecords.size() - newMatchesCount));

                    if (newMatchesCount > matchesCount) {
                        matchesCount = newMatchesCount;
                        matches = newMatches;
                        bestIteration = iteration;
                    }
                }
            }
        }

        if (matches != null) {
            int minScore = matches.values().stream()
                    .filter(Objects::nonNull)
                    .map(MatchInternal::getMatchScore)
                    .min(Integer::compare).orElse(0);
            LOGGER.finer(String.format("transactions=%d; matched=%d; unclaimed=%d; min_score=%d; %s",
                transactionCount, transactionCount - orderedTransactions.size(),
                orderedTransactions.size(), minScore, bestIteration));

            // Apply matches.
            return matches.entrySet().stream().map(e -> {
                if (e.getValue() != null) {
                    return merger.apply(e.getValue().createMatch(e.getKey()));
                }
                return e.getKey();
            });
        }

        return srcRecords.stream();
    }

    public Record mapRecord(Match match, Function<T, String> newDescription,
            Function<T, Set<String>> newTags, Logger logger) {
        RecordBuilder rb = RecordBuilder.from(match.getKey());
        rb.setDescription(newDescription.apply(match.getValue()));

        StringBuilder logMsg = null;
        if (logger.isLoggable(Level.INFO)) {
            logMsg = new StringBuilder();
            logMsg.append(String.format("Set [%s] description",
                    rb.getDescription()));
        }

        Set<String> tags = Stream.of(newTags.apply(match.getValue()), match.getTags())
                .filter(Objects::nonNull)
                .flatMap(x -> x.stream())
                .collect(Collectors.toSet());

        if (tags != null) {
            Set<String> oldTags = null;
            if (logger.isLoggable(Level.INFO)) {
                if (rb.getTags() != null) {
                    oldTags = Set.of(rb.getTags());
                } else {
                    oldTags = Set.of();
                }
            }

            tags.forEach(rb::addTag);

            if (logger.isLoggable(Level.INFO)) {
                Set<String> addedTags = new HashSet<>(Set.of(rb.getTags()));
                addedTags.removeAll(oldTags);

                if (!addedTags.isEmpty()) {
                    logMsg.append(String.format("; add %s tags", addedTags));
                }
            }
        }

        logger.info(String.format("%s in [%s]", logMsg, match));

        return rb.create();
    }

    public Stream<? extends T> getUnclaimedTransactions() {
        return orderedTransactions.stream();
    }

    private MatchInternal findTransaction(Record record) {
        Transactions transactions = transactionsMap.get(record.getCurrency());
        if (transactions != null) {
            return transactions.bestMatch(record);
        }

        return null;
    }

    private int calculateMatchScore(T a, Record b) {
        return iteration.calculateMatchScore(a, b);
    }

    private LocalDate getDate(Object o) {
        if (o instanceof Record) {
            return ((Record)o).getTransactionDate();
        }
        if (o instanceof LocalDate) {
            return (LocalDate)o;
        }
        return getDate.apply((T)o);
    }

    private BigDecimal getAmount(Object o) {
        if (o instanceof Record) {
            return ((Record)o).getAmount();
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal)o;
        }
        return getAmount.apply((T)o);
    }

    @SuppressWarnings("empty-statement")
    private static <T> int[] getRange(int idx, List<T> transactions,
            Comparator<Object> sorter) {
        final T key = transactions.get(idx);
        int left = idx;
        for (; left > 0 && sorter.compare(key, transactions.get(left - 1)) == 0;
                --left);

        int right = idx + 1;
        for (; right != transactions.size() && sorter.compare(key,
                transactions.get(right)) == 0; ++right);

        return new int[]{left, right};
    }

    private static <T> int[] findRange(Object leftKey, Object rightKey,
            Comparator<Object> sorter, List<T> transactions) {
        int leftIdx = Collections.binarySearch(transactions, leftKey, sorter);
        if (leftIdx < 0) {
            leftIdx = Math.abs(leftIdx) - 1;
        }

        int rightIdx = Collections.binarySearch(transactions, rightKey, sorter);
        if (rightIdx < 0) {
            rightIdx = Math.abs(rightIdx) - 2;
        }

        int result[] = new int[2];
        if (leftIdx < rightIdx) {
            result[0] = getRange(leftIdx, transactions, sorter)[0];
            result[1] = getRange(rightIdx, transactions, sorter)[1];
        } else if (leftIdx == rightIdx) {
            result = getRange(leftIdx, transactions, sorter);
        }

        return result;
    }

    private final Function<T, BigDecimal> getAmount;
    private final Function<T, LocalDate> getDate;
    private final Map<Currency, Transactions> transactionsMap;
    private final Collection<? extends T> orderedTransactions;
    private Predicate<Record> recordFilter;
    private Iteration iteration;
    private IntRange maxDaysRange;
    private IntRange maxAmountRange;
    private IntRange balanceRange;
    private String amountDiffTag;
    private String daysDiffTag;

    final static MathContext MC = MathContext.DECIMAL32;

    private final static Logger LOGGER = LoggingRecordMapper.LOGGER;

    private class Transactions {
        final List<T> amountSorted;
        final List<T> dateSorted;

        Transactions() {
            amountSorted = new ArrayList<>();
            dateSorted = new ArrayList<>();
        }

        private MatchInternal bestMatch(Record key) {
            final int[] amountRange = findAmountRange(key);
            final int[] dateRange = findDateRange(key);

            final Set<T> transactions = new HashSet<>(amountSorted.subList(
                    amountRange[0], amountRange[1]));
            transactions.retainAll(dateSorted.subList(dateRange[0], dateRange[1]));

            return transactions.stream()
            .map(t -> new MatchInternal(t, calculateMatchScore(t, key), this))
            .filter(m -> m.getMatchScore() != 0)
            .max((x, y) -> Math.max(x.getMatchScore(), y.getMatchScore()))
            .orElse(null);
        }

        private int[] findAmountRange(Record key) {
            final BigDecimal theKey = getAmount(key);
            final BigDecimal keyA = theKey.multiply(BigDecimal.ONE.subtract(
                    iteration.maxAmountDiff, MC), MC);
            final BigDecimal keyB = theKey.multiply(BigDecimal.ONE.add(
                    iteration.maxAmountDiff, MC), MC);
            final BigDecimal leftKey, rightKey;
            if (theKey.compareTo(BigDecimal.ZERO) > 0) {
                leftKey = keyA;
                rightKey = keyB;
            } else {
                leftKey = keyB;
                rightKey = keyA;
            }

            return findRange(
                    leftKey,
                    rightKey,
                    (x, y) -> getAmount(x).compareTo(getAmount(y)),
                    amountSorted);
        }

        private int[] findDateRange(Record key) {
            final LocalDate theKey = getDate(key);
            return findRange(
                    theKey.minusDays(iteration.maxDaysDiff),
                    theKey.plusDays(iteration.maxDaysDiff),
                    (x, y) -> getDate(x).compareTo(getDate(y)),
                    dateSorted);
        }
    }

    private final class MatchInternal {
        MatchInternal(T value, int score, Transactions transactions) {
            Objects.requireNonNull(value);
            Objects.requireNonNull(transactions);
            this.value = value;
            this.score = score;
            this.transactions = transactions;
        }

        T getValue() {
            return value;
        }

        int getMatchScore() {
            return score;
        }

        boolean isFullMatch() {
            return getMatchScore() == MAX_MATCH_SCORE;
        }

        void removeValue() {
            if (transactions != null) {
                transactions.amountSorted.remove(value);
                transactions.dateSorted.remove(value);
                transactions = null;
                orderedTransactions.remove(value);
            }
        }

        Match createMatch(Record key) {
            return new Match(score, key, value);
        }

        private final T value;
        private final int score;
        private Transactions transactions;
    };

    private final class Iteration {
        Iteration(int maxDaysDiff, int maxAmountDiff, int balance) {
            if (maxAmountDiff > MAX_AMOUNT_DIFF) {
                throw new IllegalArgumentException();
            }

            if (balance > MAX_BALANCE) {
                throw new IllegalArgumentException();
            }

            this.maxDaysDiff = maxDaysDiff;
            this.maxAmountDiff = new BigDecimal(maxAmountDiff).divide(
                    new BigDecimal(MAX_AMOUNT_DIFF), MC);
            this.balance = new BigDecimal(balance).divide(new BigDecimal(
                    MAX_BALANCE), MC);
        }

        int calculateMatchScore(T a, Record b) {
            BigDecimal aAmount = getAmount(a);
            BigDecimal bAmount = getAmount(b);
            BigDecimal amountDelta = aAmount.subtract(bAmount, MC).abs();

            final BigDecimal amountMatchDiff = amountDelta.divide(aAmount.max(
                    bAmount), MC);
            if (amountMatchDiff.compareTo(maxAmountDiff) > 0) {
                return 0;
            }

            final BigDecimal amountMatchScore = BigDecimal.ONE.subtract(
                    amountMatchDiff, MC);

            final long daysDiff = Math.abs(ChronoUnit.DAYS.between(getDate(a),
                    getDate(b)));

            if (daysDiff > maxDaysDiff) {
                return 0;
            }

            final BigDecimal dateMatchScore = BigDecimal.ONE.subtract(
                    new BigDecimal(daysDiff).divide(new BigDecimal(maxDaysDiff
                            + 1), MC), MC);

            final BigDecimal amountMatch = amountMatchScore.multiply(balance, MC);
            final BigDecimal dateMatch = dateMatchScore.multiply(
                    BigDecimal.ONE.subtract(balance, MC), MC);

            final BigDecimal match = amountMatch.add(dateMatch);

            return match.multiply(new BigDecimal(MAX_MATCH_SCORE), MC).intValue();
        }

        Map<Record, MatchInternal> mapRecords(Stream<Record> records) {
            Map<T, List<Record>> mappedEntries = new HashMap<>();
            Map<Record, MatchInternal> matches = new LinkedHashMap<>();

            Consumer<Record> initializer = record -> {
                final MatchInternal match;
                if (recordFilter == null || recordFilter.test(record)) {
                    match = findTransaction(record);
                } else {
                    match = null;
                }
                if (match != null) {
                    if (match.isFullMatch()) {
                        match.removeValue();
                    } else {
                        List<Record> mappedRecords = mappedEntries.get(
                                match.getValue());
                        if (mappedRecords == null) {
                            mappedRecords = new ArrayList<>(List.of(record));
                            mappedEntries.put(match.getValue(), mappedRecords);
                        }
                    }
                }
                matches.put(record, match);
            };

            // Collect all matches.
            records.forEachOrdered(initializer);

            List<Record> collisions = new ArrayList<>();
            do {
                // Resolve match collisions.
                // Order matches by score. Start with the best match.
                matches.values().stream()
                        .filter(Objects::nonNull)
                        .filter(Predicate.not(MatchInternal::isFullMatch))
                        .sorted(Comparator.comparingInt(MatchInternal::getMatchScore).reversed())
                        .forEachOrdered(match -> {
                    List<Record> candidates = mappedEntries.get(match.getValue());
                    if (candidates != null) {
                        Record bestMatch = Collections.max(candidates,
                                (x, y) -> calculateMatchScore(match.getValue(), x)
                                - calculateMatchScore(match.getValue(), y));
                        match.removeValue();
                        mappedEntries.remove(match.getValue());
                        candidates.stream()
                                .filter(Predicate.not(bestMatch::equals))
                                .forEachOrdered(collisions::add);
                    }
                });
                collisions.forEach(initializer);
            } while (!collisions.isEmpty());

            return matches;
        }

        @Override
        public String toString() {
            return String.format("amount-diff=%s; days-diff=%d; balance=%s",
                    maxAmountDiff, maxDaysDiff, balance);
        }

        private final long maxDaysDiff;
        private final BigDecimal maxAmountDiff;
        private final BigDecimal balance;
    }
}
