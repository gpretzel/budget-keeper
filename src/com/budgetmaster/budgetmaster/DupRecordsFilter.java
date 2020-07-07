package com.budgetmaster.budgetmaster;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class DupRecordsFilter implements UnaryOperator<Stream<Record>> {

    @Override
    public Stream<Record> apply(Stream<Record> records) {
        return records.collect(Collectors.toMap(Record::getGlobalId,
                Function.identity(), (existing, replacement) -> replacement)).values().stream();
    }
}
