package com.budgetmaster.budgetmaster;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;


class DateRangeMatcher implements Predicate<Record> {
    DateRangeMatcher(LocalDate begin, LocalDate end,
            Function<Record, LocalDate> dateAccessor, boolean inclusive) {
        Objects.requireNonNull(begin);
        Objects.requireNonNull(end);

        this.begin = begin;
        this.end = end;
        this.dateAccessor = dateAccessor;
        this.inclusive = inclusive;

        if (begin.isAfter(end)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid date range: %s - %s", begin, end));
        }
    }

    @Override
    public boolean test(Record t) {
        LocalDate v = dateAccessor.apply(t);
        boolean after = v.isAfter(begin) || (inclusive && v.isEqual(begin));
        boolean before = v.isBefore(end) || (inclusive && v.isEqual(end));

        return after && before;
    }

    private final boolean inclusive;
    private final LocalDate begin;
    private final LocalDate end;
    private final Function<Record, LocalDate> dateAccessor;
}
