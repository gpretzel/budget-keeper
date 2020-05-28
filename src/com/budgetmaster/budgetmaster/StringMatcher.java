package com.budgetmaster.budgetmaster;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class StringMatcher implements Predicate<Record> {
    StringMatcher(String regexp, Function <Record, String> fieldAccessor) {
        Objects.requireNonNull(regexp);
        Objects.requireNonNull(fieldAccessor);

        this.regexp = Pattern.compile(regexp);
        this.fieldAccessor = fieldAccessor;
    }

    @Override
    public boolean test(Record t) {
        String v = fieldAccessor.apply(t);
        if (v == null) {
            return false;
        }
        Matcher m = regexp.matcher(v);
        return m.find();
    }

    private final Function <Record, String> fieldAccessor;
    private final Pattern regexp;
}
