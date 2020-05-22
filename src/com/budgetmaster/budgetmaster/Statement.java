package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingSupplier;
import java.nio.file.Path;
import java.util.Currency;
import java.util.stream.Stream;


interface Statement extends ThrowingSupplier<Stream<Record>> {
    String getId();
    String getSystemId();

    static Statement fromStatementFile(String id, Path path, Currency currency,
            RecordSupplier rs) {
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
            public Stream<Record> get() throws Throwable {
                Stream<Record> records = rs.read(path);

                Stream<RecordBuilder> rbStream = records.map(RecordBuilder::from).peek(
                        rb -> {
                            if (rb.getSource() == null) {
                                rb.setSource(this);
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
}
