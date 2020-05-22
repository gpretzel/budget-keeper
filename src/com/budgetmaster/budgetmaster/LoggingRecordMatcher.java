package com.budgetmaster.budgetmaster;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;


final class LoggingRecordMatcher implements Predicate<Record> {
    static Predicate<Record> of(Predicate<Record> what, String msg) {
        return new LoggingRecordMatcher(what, msg);
    }
    
    private LoggingRecordMatcher(Predicate<Record> what, String msg) {
        Objects.requireNonNull(what);
        Objects.requireNonNull(msg);

        this.what = what;
        this.msg = msg;
    }

    @Override
    public boolean test(Record t) {
        boolean matched = what.test(t);
        if (matched) {
            LOGGER.info(String.format("%s matched [%s]", msg, t));
        }
        return matched;
    }

    private final Predicate<Record> what;
    private final String msg;

    static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());
}
