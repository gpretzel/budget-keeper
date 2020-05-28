package com.budgetmaster.budgetmaster;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;


final class LoggingRecordMapper implements UnaryOperator<Record> {
    static UnaryOperator<Record> of(UnaryOperator<Record> what,
            String msg) {
        return new LoggingRecordMapper(what, msg);
    }

    private LoggingRecordMapper(UnaryOperator<Record> what, String msg) {
        Objects.requireNonNull(what);
        Objects.requireNonNull(msg);
        this.what = what;
        this.msg = msg;
    }

    @Override
    public Record apply(Record r) {
        LOGGER.info(String.format("%s [%s]", msg, r));
        return what.apply(r);
    }

    private final UnaryOperator<Record> what;
    private final String msg;

    static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());
}
