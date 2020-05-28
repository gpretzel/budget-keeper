package com.budgetmaster.budgetmaster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;


final class StatementOverlapFinder implements Function<Stream<Record>, Collection<Entry<String, String>>> {

    @Override
    public Collection<Entry<String, String>> apply(Stream<Record> records) {
        // Cluster by statement type and order by date
        records = records.sorted((r1, r2) -> {
            int result = r1.getSource().getId().compareTo(r2.getSource().getId());
            if (result == 0) {
                result = r1.getPostingDate().compareTo(r2.getPostingDate());
            }
            return result;
        });

        State state = new State();
        records.forEachOrdered(record -> {
            if (state.prevRecord == null) {
                state.processedStatements.add(record.getSource().getSystemId());
            } else if (state.prevRecord.getSource().getSystemId().equals(
                    record.getSource().getSystemId())) {
            } else if (state.processedStatements.contains(
                    record.getSource().getSystemId())) {
                String r1SystemId = record.getSource().getSystemId();
                String r2SystemId = state.prevRecord.getSource().getSystemId();
                if (r1SystemId.compareTo(r2SystemId) < 0) {
                    state.overlappedPairs.add(Map.entry(r1SystemId, r2SystemId));
                } else {
                    state.overlappedPairs.add(Map.entry(r2SystemId, r1SystemId));
                }
            } else {
                state.processedStatements.add(record.getSource().getSystemId());
            }
            state.prevRecord = record;
        });

        return state.overlappedPairs;
    }

    private static final class State {
        Set<String> processedStatements = new HashSet<>();
        Set<Entry<String, String>> overlappedPairs = new HashSet<>();
        Record prevRecord;
    }
}
