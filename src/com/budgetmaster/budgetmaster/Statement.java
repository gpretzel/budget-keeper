package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingSupplier;
import java.nio.file.Path;
import java.util.Currency;
import java.util.stream.Stream;


interface Statement extends ThrowingSupplier<Stream<Record>> {
    String getId();
    String getSystemId();
    Currency getCurrency();
    
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
            public Currency getCurrency() {
                return currency;
            }

            @Override
            public Stream<Record> get() throws Throwable {
                Stream<Record> records = rs.read(path);
                return records  .map(RecordBuilder::from)
                                .peek(rb -> rb.setSource(this))
                                .map(RecordBuilder::create);
            }
        };
    }
}
