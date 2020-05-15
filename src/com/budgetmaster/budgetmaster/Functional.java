package com.budgetmaster.budgetmaster;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class Functional {
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Throwable;

        public static <T> Consumer<T> toConsumer(ThrowingConsumer<T> v) {
            return o -> {
                try {
                    v.accept(o);
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Throwable;

        public static <T> Supplier<T> toSupplier(ThrowingSupplier<T> v) {
            return () -> {
                try {
                    return v.get();
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
                // Unreachable
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Throwable;

        public static <T, R> Function<T, R> toFunction(ThrowingFunction<T, R> v) {
            return (t) -> {
                try {
                    return v.apply(t);
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
                // Unreachable
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;

        public static Runnable toRunnable(ThrowingRunnable v) {
            return () -> {
                try {
                    v.run();
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
            };
        }
    }

    public static <T> Supplier<T> identity(Supplier<T> v) {
        return v;
    }

    public static <T> Consumer<T> identity(Consumer<T> v) {
        return v;
    }

    public static Runnable identity(Runnable v) {
        return v;
    }

    public static <T, R> Function<T, R> identity(Function<T, R> v) {
        return v;
    }

    public static <T, R> Function<T, R> identityFunction(Function<T, R> v) {
        return v;
    }

    public static <T> Predicate<T> identity(Predicate<T> v) {
        return v;
    }

    public static <T> Predicate<T> identityPredicate(Predicate<T> v) {
        return v;
    }

    public static class ExceptionBox extends RuntimeException {
        public ExceptionBox(Throwable throwable) {
            super(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    public static void rethrowUnchecked(Throwable throwable) throws ExceptionBox {
        if (throwable instanceof ExceptionBox) {
            throw (ExceptionBox)throwable;
        }

        if (throwable instanceof InvocationTargetException) {
            new ExceptionBox(throwable.getCause());
        }

        throw new ExceptionBox(throwable);
    }
}
