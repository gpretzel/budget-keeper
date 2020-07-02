package com.budgetmaster.budgetmaster;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public interface RecordsMapper extends UnaryOperator<Stream<Record>> {

    default public void onRecordsProcessed() {}
}
