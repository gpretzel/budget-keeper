package com.budgetmaster.budgetmaster;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class MarketplaceTransactions<T> {
    public final static int MAX_MATCH_SCORE = 1000;
    public final static int MIN_MATCH_SCORE = 0;

    public final class Match {
        Match(int score, Record key, T value) {
            if (score <= MIN_MATCH_SCORE) {
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

        @Override
        public String toString() {
            final BigDecimal amountDiff = getAmount(key).subtract(getAmount(
                    value));
            final long daysDiff = ChronoUnit.DAYS.between(getDate(key), getDate(
                    value));

            StringBuilder sb = new StringBuilder();
            if (amountDiff.compareTo(BigDecimal.ZERO) != 0) {
                sb.append(String.format(" amount-diff=%s;", amountDiff));
            }
            if (daysDiff != 0) {
                sb.append(String.format(" days-diff=%s;", daysDiff));
            }
            return String.format("score=%d;%s key=%s; value=%s", score, sb, key,
                    value);
        }

        private final Record key;
        private final T value;
        private final int score;
    };

    public MarketplaceTransactions(Stream<T> transactions,
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
    }

    public MarketplaceTransactions<T> setMaxDaysDiff(long v) {
        maxDaysDiff = v;
        return this;
    }

    public MarketplaceTransactions<T> setMaxAmountDiff(double v) {
        maxAmountDiff = v;
        return this;
    }

    public Stream<Record> filterRecords(Stream<Record> records,
            Function<Match, Record> merger) {
        Map<T, List<Record>> mappedEntries = new HashMap<>();
        Map<Record, MatchInternal> matches = new LinkedHashMap<>();

        Consumer<Record> initializer = record -> {
            MatchInternal match = findTransaction(record);
            if (match != null) {
                if (match.isFullMatch()) {
                    match.removeValue();
                } else {
                    List<Record> mappedRecords = mappedEntries.get(match.getValue());
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

        // Resorve match collisions.
        matches.values().forEach(match -> {
            if (match != null && !match.isFullMatch()) {
                List<Record> candidates = mappedEntries.get(match.getValue());
                if (candidates != null) {
                    Record bestMatch = Collections.max(candidates,
                            (x, y) -> calculateMatchScore(match.getValue(), x)
                            - calculateMatchScore(match.getValue(), y));
                    match.removeValue();
                    mappedEntries.remove(match.getValue());
                    candidates.stream()
                            .filter(Predicate.not(bestMatch::equals))
                            .forEach(initializer);
                }
            }
        });

        // Apply matches.
        return matches.entrySet().stream().map(e -> {
            if (e.getValue() != null) {
                return merger.apply(e.getValue().createMatch(e.getKey()));
            }
            return e.getKey();
        });
    }

    public Stream<T> getUnclaimedTransactions() {
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
        BigDecimal aAmount = getAmount(a);
        BigDecimal bAmount = getAmount(b);
        BigDecimal amountDelta = aAmount.subtract(bAmount).abs();

        final double amountMatchDiff = amountDelta.divide(aAmount.max(
                bAmount), RoundingMode.HALF_UP).doubleValue();
        if (amountMatchDiff > maxAmountDiff) {
            return MIN_MATCH_SCORE;
        }

        final double amountMatchScore = 1.0 - amountMatchDiff;

        final long daysDiff = Math.abs(ChronoUnit.DAYS.between(getDate(a),
                getDate(b)));

        if (daysDiff > maxDaysDiff) {
            return MIN_MATCH_SCORE;
        }

        final double dateMatchScore = 1.0 - ((double) daysDiff) / (maxDaysDiff
                + 1);

        return (int) (MIN_MATCH_SCORE + amountMatchScore * dateMatchScore
                * (MAX_MATCH_SCORE - MIN_MATCH_SCORE));
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
    private final Collection<T> orderedTransactions;
    private long maxDaysDiff;
    private double maxAmountDiff;

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
            final BigDecimal keyA = theKey.multiply(new BigDecimal(1.0
                    - maxAmountDiff));
            final BigDecimal keyB = theKey.multiply(new BigDecimal(1.0
                    + maxAmountDiff));
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
                    theKey.minusDays(maxDaysDiff),
                    theKey.plusDays(maxDaysDiff),
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
}
