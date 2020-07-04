package com.budgetmaster.budgetmaster;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class Functional {
    @FunctionalInterface
    public interface ThrowingConsumer<T> {

        void accept(T t) throws Exception;

        public static <T> Consumer<T> toConsumer(ThrowingConsumer<T> v) {
            return o -> {
                try {
                    v.accept(o);
                } catch (Exception ex) {
                    rethrowUnchecked(ex);
                }
            };
        }
    }
    
    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {

        void accept(T t, U u) throws Exception;

        public static <T, U> BiConsumer<T, U> toBiConsumer(
                ThrowingBiConsumer<T, U> v) {
            return (t, u) -> {
                try {
                    v.accept(t, u);
                } catch (Exception ex) {
                    rethrowUnchecked(ex);
                }
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        T get() throws Exception;

        public static <T> Supplier<T> toSupplier(ThrowingSupplier<T> v) {
            return () -> {
                try {
                    return v.get();
                } catch (Exception ex) {
                    rethrowUnchecked(ex);
                }
                // Unreachable
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {

        R apply(T t) throws Exception;

        public static <T, R> Function<T, R> toFunction(ThrowingFunction<T, R> v) {
            return (t) -> {
                try {
                    return v.apply(t);
                } catch (Exception ex) {
                    rethrowUnchecked(ex);
                }
                // Unreachable
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingBiFunction<T, U, R> {

        R apply(T t, U u) throws Exception;

        public static <T, U, R> BiFunction<T, U, R> toBiFunction(
                ThrowingBiFunction<T, U, R> v) {
            return (t, u) -> {
                try {
                    return v.apply(t, u);
                } catch (Exception ex) {
                    rethrowUnchecked(ex);
                }
                // Unreachable
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {

        void run() throws Exception;

        public static Runnable toRunnable(ThrowingRunnable v) {
            return () -> {
                try {
                    v.run();
                } catch (Exception ex) {
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
    
    public static <T, U> BiConsumer<T, U> identity(BiConsumer<T, U> v) {
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

    public static <T, U, R> BiFunction<T, U, R> identity(BiFunction<T, U, R> v) {
        return v;
    }

    public static <T, U, R> BiFunction<T, U, R> identityBiFunction(
            BiFunction<T, U, R> v) {
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
    public static void rethrowUnchecked(Exception ex) throws ExceptionBox {
        if (ex instanceof ExceptionBox) {
            throw (ExceptionBox)ex;
        }

        if (ex instanceof RuntimeException) {
            throw (RuntimeException)ex;
        }

        if (ex instanceof InvocationTargetException) {
            throw new ExceptionBox(ex.getCause());
        }

        throw new ExceptionBox(ex);
    }
}
