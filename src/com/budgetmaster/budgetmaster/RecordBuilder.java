package com.budgetmaster.budgetmaster;

import java.time.LocalDate;
import java.util.Currency;

final class RecordBuilder {
    Record create() {
        return new Record(date, filterSting(desc), filterAmount(), filterSting(
                category), source);
    }
    
    static RecordBuilder from(Record record) {
        return new RecordBuilder()
                .setAmount(record.getAmount())
                .setDescription(record.getDescription())
                .setDate(record.getDate())
                .setCategory(record.getCategory())
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
        String result = filterSting(amount);
        int sepIdx = result.indexOf(',');
        if (sepIdx > 0) {
            result = result.substring(0, sepIdx) + result.substring(sepIdx + 1);
        }
        
        if (negateAmount) {
            if (result.charAt(0) == '-') {
                result = result.substring(1);
            } else {    
                result = "-" + result;
            }
        }
        
        return result;
    }

    private boolean negateAmount;
    private boolean strip;
    private LocalDate date;
    private String desc;
    private String category;
    private Statement source;
    private String amount;
}
