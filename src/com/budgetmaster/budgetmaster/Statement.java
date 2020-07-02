package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingSupplier;
import java.nio.file.Path;
import java.util.Currency;
import java.util.Objects;
import java.util.stream.Stream;


interface Statement extends ThrowingSupplier<Stream<Record>> {
    String getId();
    String getSystemId();

    static Statement fromStatementFile(String id, Path path, Currency currency,
            RecordsSupplier rs) {
        return new Statement() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getSystemId() {
                return path.toString();
            }

            @Override
            public Stream<Record> get() throws Exception {
                Stream<Record> records = rs.read(path);

                RecordIndexSupplier index = new RecordIndexSupplier();
                Stream<RecordBuilder> rbStream = records
                        .sequential()
                        .peek(record -> index.nextId())
                        .filter(Objects::nonNull)
                        .map(RecordBuilder::from)
                        .peek(rb -> {
                            if (rb.getSource() == null) {
                                rb.setSource(this);
                            }
                            if (rb.getId() == null) {
                                rb.setId(index.getId());
                            }
                        });

                if (currency != null) {
                    rbStream = rbStream.peek(rb -> {
                        if (rb.getCurrency() == null) {
                            rb.setCurrency(currency);
                        }
                    });
                }
                return rbStream.map(RecordBuilder::create);
            }
        };
    }

    static class RecordIndexSupplier {
        void nextId() {
            ++recordIdx;
        }

        String getId() {
            return String.format("#%d", recordIdx);
        }

        private int recordIdx;
    }
}
