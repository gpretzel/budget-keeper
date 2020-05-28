package com.budgetmaster.budgetmaster;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class ExcludingRecordsFilter implements UnaryOperator<Stream<Record>> {

    public ExcludingRecordsFilter negate(boolean v) {
        negate = v;
        return this;
    }

    public ExcludingRecordsFilter maxPeriodDays(int v) {
        maxPeriodDays = v;
        return this;
    }

    public ExcludingRecordsFilter refund(boolean v) {
        refund = v;
        return this;
    }

    @Override
    public Stream<Record> apply(Stream<Record> records) {
        Map<String, List<Record>> sourceClusters = new HashMap<>();

        records.forEachOrdered(r -> {
                    String sourceId = r.getSource().getId();
                    List<Record> clusterRecords = sourceClusters.get(sourceId);
                    if (clusterRecords == null) {
                        clusterRecords = new ArrayList<>();
                        sourceClusters.put(sourceId, clusterRecords);
                    }
                    clusterRecords.add(r);
                });

        return sourceClusters.values().stream().map(this::forEachSource).flatMap(
                s -> s);
    }

    private Stream<Record> forEachSource(List<Record> records) {
        return forEachCluster(records.stream(), record -> new BigDecimal(
                record.getAmount()).abs(), this::forEachAbsAmount);
    }

    private Stream<Record> forEachAbsAmount(Stream<Record> records) {
        records = records.sorted((r1, r2) -> {
            int result = r1.getTransactionDate().compareTo(
                    r2.getTransactionDate());
            if (result == 0) {
                // Same day transactions
                int score1 = (r1.isNegative() ? 1 : 0);
                int score2 = (r2.isNegative() ? 1 : 0);
                result = score1 - score2;
            }
            return result;
        });
        return forEachAbsAmount(records.toArray(Record[]::new));
    }

    private Stream<Record> forEachAbsAmount(Record[] records) {
        List<Record> matches = null;

        for (int i = 0; i < records.length; ++i) {
            if (records[i] != null) {
                boolean isNegative = records[i].isNegative();
                if ((refund && !isNegative) || !refund) {
                    for (int j = i + 1; j < records.length; ++j) {
                        if (records[j] != null && isNegative
                                != records[j].isNegative()) {
                            final long periodDays = ChronoUnit.DAYS.between(
                                    records[i].getTransactionDate(),
                                    records[j].getTransactionDate());
                            if (periodDays < maxPeriodDays) {
                                if (!negate) {
                                    if (matches == null) {
                                        matches = new ArrayList<>();
                                    }
                                    matches.add(records[i]);
                                    matches.add(records[j]);
                                }

                                LOGGER.info(String.format(
                                        "Matching records: [%s] [%s]",
                                        records[i], records[j]));

                                records[i] = null;
                                records[j] = null;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (negate) {
            return Stream.of(records).filter(Objects::nonNull);
        }

        if (matches == null) {
            return Stream.of();
        }

        return matches.stream();
    }

    private <T> Stream<Record> forEachCluster(Stream<Record> records,
            Function<Record, T> clusterFunction,
            UnaryOperator<Stream<Record>> nextFilter) {
        // Cluster records.
        Map<T, Record[]> clusters = records.collect(Collectors.toMap(
                clusterFunction,
                record -> new Record[]{record},
                (a, b) -> Stream.concat(Stream.of(a), Stream.of(b)).toArray(
                        Record[]::new)));

        return clusters.values().stream()
                .filter(items -> items.length > 1)
                .map(Stream::of)
                .map(nextFilter)
                .flatMap(s -> s);
    }

    private boolean negate;
    private boolean refund;
    private long maxPeriodDays = Long.MAX_VALUE;

    private final static Logger LOGGER = LoggingRecordMatcher.LOGGER;
}
