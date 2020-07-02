package com.budgetmaster.budgetmaster.paypal;

import com.budgetmaster.budgetmaster.AccountStatementCsv;
import com.budgetmaster.budgetmaster.MonetaryAmount;
import com.budgetmaster.budgetmaster.Record;
import com.budgetmaster.budgetmaster.RecordBuilder;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


public final class PayPalStatementCsv extends AccountStatementCsv {
    @Override
    public Stream<Record> read(Path csvFilePath) throws IOException {
        Stream<Record> result = super.read(csvFilePath);
        if (redemptions != null) {
            result = result.filter(Objects::nonNull).map(record -> {
                BigDecimal amount = redemptions.get(new RedemptionKey(record));
                if (amount != null) {
                    LOGGER.fine(String.format(
                            "Apply redemption to [%s] record. New amount: [%s]",
                            record, amount));
                    record = RecordBuilder.from(record).setAmount(amount).create();
                }
                return record;
            });
        }
        return result;
    }

    @Override
    protected Map<RecordBuilder.Setter, Enum<?>> fieldMapper() {
        return Map.of(
                RecordBuilder.Setter.Amount, Headers.Amount,
                RecordBuilder.Setter.TransactionDate, Headers.Date,
                RecordBuilder.Setter.Currency, Headers.Currency,
                RecordBuilder.Setter.Description, Headers.Name,
                RecordBuilder.Setter.Category, Headers.Type
        );
    }
    
    @Override
    protected void customReadRecord(RecordBuilder rb, CSVRecord record) {
        if ("General Incentive/Certificate Redemption".equals(record.get(
                Headers.Type)) && "eBay Inc.".equals(record.get(Headers.Name))) {
            // This is redemption record.
            
            if (redemptions == null) {
                redemptions = new HashMap<>();
            }
            
            LOGGER.finer(String.format("Redemption record: %s", record));

            BigDecimal redemption = MonetaryAmount.of(record.get(Headers.Amount));
            BigDecimal balance = MonetaryAmount.of(record.get(Headers.Balance));
            BigDecimal amount = balance.subtract(redemption);
            
            rb.setAmount(amount);
            redemptions.put(new RedemptionKey(rb), balance);
            
            // This record is not needed.
            rb.setAmount((BigDecimal)null);
        }
    }

    @Override
    protected DateTimeFormatter recordDateTimeFormatter() {
        return DATE_FORMATTER;
    }

    @Override
    protected CSVFormat initParser() {
        return CSVFormat.RFC4180.withFirstRecordAsHeader().withHeader(Headers.class);
    }
    
    private enum Headers {
        Date,
        Time,
        TimeZone,
        Name,
        Type,
        Status,
        Currency,
        Amount,
        Receipt_ID,
        Balance
    };
    
    private final static class RedemptionKey {
        RedemptionKey(Record v) {
            this.amount = v.getAmount();
            this.currency = v.getCurrency();
            this.date = v.getTransactionDate();
        }
        
        RedemptionKey(RecordBuilder v) {
            this.amount = v.getAmount();
            this.currency = v.getCurrency();
            this.date = v.getTransactionDate();
        }
        
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null) {
                return false;
            }
            if (getClass() != other.getClass()) {
                return false;
            }
            
            RedemptionKey rk = (RedemptionKey)other;
            return rk.amount.equals(amount) && rk.currency.equals(currency)
                    && rk.date.equals(date);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(this.amount);
            hash = 79 * hash + Objects.hashCode(this.currency);
            hash = 79 * hash + Objects.hashCode(this.date);
            return hash;
        }
        
        private final BigDecimal amount;
        private final Currency currency;
        private final LocalDate date;
    }
    
    private Map<RedemptionKey, BigDecimal> redemptions;

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "MM/dd/yyyy");
    
    private static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());
}
