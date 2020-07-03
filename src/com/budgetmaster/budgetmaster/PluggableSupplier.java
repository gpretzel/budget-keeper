package com.budgetmaster.budgetmaster;

import java.util.function.Supplier;

public interface PluggableSupplier<T> extends Supplier<T>, Pluggable {
}
