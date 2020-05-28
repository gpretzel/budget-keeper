package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;


public interface RecordSupplier {
    public Stream<Record> read(Path file) throws IOException;
}
