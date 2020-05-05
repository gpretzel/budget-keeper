package com.budgetmaster.budgetmaster;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;


class RecordFieldMapper implements UnaryOperator<Record> {
    RecordFieldMapper(String value,
            BiConsumer<RecordBuilder, String> fieldAccessor) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(fieldAccessor);

        this.value = (record) -> value;
        this.fieldAccessor = fieldAccessor;
    }

    RecordFieldMapper(Function<Record, String> value,
            BiConsumer<RecordBuilder, String> fieldAccessor) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(fieldAccessor);

        this.value = value;
        this.fieldAccessor = fieldAccessor;
    }
    
    static RecordFieldMapper amountNegator() {
        return new RecordFieldMapper((String)null, (rb, amount) -> {
            rb.negateAmount();
        });
    }
    
    @Override
    public Record apply(Record r) {
        RecordBuilder rb = RecordBuilder.from(r);
        fieldAccessor.accept(rb, value.apply(r));
        return rb.create();
    }

    private final BiConsumer <RecordBuilder, String> fieldAccessor;
    private final Function<Record, String> value;
}
