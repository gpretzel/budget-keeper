package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


public interface RecordSupplier {
    public List<Record> read(Path file) throws IOException;
}
