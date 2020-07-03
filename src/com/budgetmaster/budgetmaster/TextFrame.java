package com.budgetmaster.budgetmaster;

import java.util.Objects;


public final class TextFrame {
    public TextFrame(String v) {
        Objects.requireNonNull(v);
        this.data = v;
    }

    public TextFrame set(String from, String to) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        int newBegin = data.indexOf(from, begin);
        if (newBegin < 0) {
            throw new TextFrameException(String.format(
                    "Can't find [%s] substring in [%s] after %d code unit", from, data,
                    begin));
        }
        int newEnd = data.indexOf(to, newBegin + from.length());
        if (newEnd < 0) {
            throw new TextFrameException(String.format(
                    "Can't find [%s] substring following [%s] substring in [%s] after %d code unit",
                    to, from, data, newBegin));
        }

        begin = newBegin + from.length();
        end = newEnd;

        return this;
    }

    public String getValue() {
        return data.substring(begin, end);
    }

    public String getStrippedValue() {
        return getValue().strip();
    }

    private final String data;
    private int begin;
    private int end;
}
