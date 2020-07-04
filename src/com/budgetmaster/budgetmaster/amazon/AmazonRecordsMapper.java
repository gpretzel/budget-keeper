package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.Functional.ThrowingConsumer;
import com.budgetmaster.budgetmaster.LoggingRecordMapper;
import com.budgetmaster.budgetmaster.MarketplaceTransactions;
import com.budgetmaster.budgetmaster.MonetaryAmount;
import com.budgetmaster.budgetmaster.Record;
import java.util.stream.Stream;
import com.budgetmaster.budgetmaster.RecordsMapper;
import com.budgetmaster.budgetmaster.amazon.AmazonRecord.CsvOrderHeaders;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;


final class AmazonRecordsMapper implements RecordsMapper {
    AmazonRecordsMapper(AmazonRawRecords orders, AmazonRawRecords refunds,
            Set<String> tags, boolean addOrderIdTag,
            Consumer<MarketplaceTransactions<?>> transactionsInitializer,
            Path unclaimedOrdersCsvFile, Path unclaimedRefundsCsvFile) {
        this.transactionsInitializer = transactionsInitializer;
        this.rawOrders = orders;
        this.rawRefunds = refunds;
        this.tags = tags;
        this.addOrderIdTag = addOrderIdTag;
        this.unclaimedOrdersCsvFile = unclaimedOrdersCsvFile;
        this.unclaimedRefundsCsvFile = unclaimedRefundsCsvFile;
    }

    @Override
    public void onRecordsProcessed() {
        final Predicate<AmazonRawRecord> filter;
        if (transactions == null) {
            filter = x -> true;
        } else {
            filter = transactions.getUnclaimedTransactions()
                    .map(LinkedRecord.class::cast)
                    .map(LinkedRecord::getRawValue)                
                    .collect(Collectors.toSet())::contains;
        }
        
        List.of(
                Map.entry(rawOrders, unclaimedOrdersCsvFile), 
                Map.entry(rawRefunds, unclaimedRefundsCsvFile)
        ).forEach(ThrowingConsumer.toConsumer(e -> {
            Path outputCsvFile = e.getValue();
            if (outputCsvFile != null) {
                e.getKey().save(outputCsvFile, filter);
            }
        }));        
    }

    @Override
    public Stream<Record> apply(Stream<Record> records) {
        // Group orders by order ID and create grouped orders.
        transactions = createTransactions(rawOrders.getData().map(
                this::createOrder).collect(Collectors.groupingBy(
                        AmazonRecord::getOrderId)).values().stream().map(
                        this::createOrder));
        
        Set<Record> matchedRecords = new HashSet<>();
        
        // Run pass #1 on grouped orders.
        var newRecords = apply(records, transactions, record -> {
            synchronized(matchedRecords) {
                matchedRecords.add(record);
            }
        }).collect(Collectors.toList());

        Set<String> retryOrderIDs = transactions.getUnclaimedTransactions().map(
                AmazonRecord::getOrderId).collect(Collectors.toSet());
        
        // Orders.
        var linkedOrders = rawOrders.getData()
                .map(this::createOrder)
                .filter(o -> retryOrderIDs.contains(o.getOrderId()));

        // Refunds.
        var linkedRefunds = rawRefunds.getData().map(this::createRefund);
        
        // Run pass #2 on orders and refunds.
        transactions = createTransactions(
                Stream.of(linkedOrders, linkedRefunds).flatMap(x -> x));
        transactions.setRecordFilter(Predicate.not(matchedRecords::contains));
        
        return apply(newRecords.stream(), transactions, null);
    }
    
    private Stream<Record> apply(Stream<Record> records,
            MarketplaceTransactions<LinkedRecord> transactions,
            Consumer<Record> matchedRecordConsumer) {
        return transactions.mapRecords(records, match -> {
            if (matchedRecordConsumer != null) {
                matchedRecordConsumer.accept(match.getKey());
            }
            final Set<String> addTags;
            if (addOrderIdTag) {
                addTags = new HashSet<>(tags);
                addTags.add(match.getValue().getOrderId());
            } else {
                addTags = tags;
            }
            return transactions.mapRecord(match, AmazonRecord::getTitle,
                    v -> addTags, LOGGER);
        });
    }

    private MarketplaceTransactions<LinkedRecord> createTransactions(
            Stream<LinkedRecord> data) {
        var result = new MarketplaceTransactions<>(data, LinkedRecord::getAmount,
                LinkedRecord::getCurrency, LinkedRecord::getDate);
        transactionsInitializer.accept(result);
        return result;
    }

    private LinkedRecord createRecord(AmazonRecord impl, AmazonRawRecord rawValue,
            Currency currency) {
        return new LinkedRecord() {
            @Override
            public AmazonRawRecord getRawValue() {
                return rawValue;
            }

            @Override
            public Currency getCurrency() {
                return currency;
            }

            @Override
            public String getOrderId() {
                return impl.getOrderId();
            }

            @Override
            public LocalDate getDate() {
                return impl.getDate();
            }

            @Override
            public String getTitle() {
                return impl.getTitle();
            }

            @Override
            public BigDecimal getAmount() {
                return impl.getAmount();
            }
            
            @Override
            public String toString() {
                return impl.toString();
            }
        };
    }

    private LinkedRecord createOrder(AmazonRawRecord rawOrder) {
        String currencyStr = rawOrder.getData()[CsvOrderHeaders.Currency.ordinal()];
        final Currency currency = Currency.getInstance(currencyStr);
        return createRecord(AmazonRecord.order(rawOrder), rawOrder, currency);
    }

    private LinkedRecord createOrder(List<LinkedRecord> orders) {
        if (orders.size() == 1) {
            return orders.get(0);
        }
        return createRecord(AmazonRecord.order(orders), null,
                orders.get(0).getCurrency());
    }
    
    private LinkedRecord createRefund(AmazonRawRecord rawRefund) {
        final Currency currency = MonetaryAmount.of(
                rawRefund.getData()[AmazonRecord.CsvRefundHeaders.RefundAmount.ordinal()]).getCurrency();
        return createRecord(AmazonRecord.refund(rawRefund), rawRefund, currency);
    }

    private static interface LinkedRecord extends AmazonRecord {
        AmazonRawRecord getRawValue();
        Currency getCurrency();
    }

    private final Consumer<MarketplaceTransactions<?>> transactionsInitializer;
    private MarketplaceTransactions<LinkedRecord> transactions;
    private final Set<String> tags;
    private final boolean addOrderIdTag;
    private final AmazonRawRecords rawOrders;
    private final AmazonRawRecords rawRefunds;
    private final Path unclaimedOrdersCsvFile;
    private final Path unclaimedRefundsCsvFile;

    private final static Logger LOGGER = LoggingRecordMapper.LOGGER;
}
