package com.budgetmaster.budgetmaster;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class MonetaryAmount {

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public Currency getCurrency() {
        return SYMBOL_TO_CURRENCY.get(currencySymbol);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public static MonetaryAmount of(String value) {
        return new MonetaryAmount(value);
    }

    private MonetaryAmount(String value) {
        value = value.strip();
        final boolean negative = value.charAt(0) == '-';
        int idx = 0;
        while (idx < value.length() && !Character.isDigit(value.charAt(idx))) {
            idx++;
        }

        if (negative) {
            currencySymbol = value.substring(1, idx);
            amount = parse(value.substring(idx)).negate();
        } else {
            currencySymbol = value.substring(0, idx);
            amount = parse(value.substring(idx));
        }
    }

    private static BigDecimal parse(String value) {
        int sepIdx = value.indexOf(',');
        if (sepIdx > 0) {
            value = value.substring(0, sepIdx) + value.substring(sepIdx + 1);
        }

        return new BigDecimal(value).stripTrailingZeros();
    }

    private final String currencySymbol;
    private final BigDecimal amount;

    private static final Map<String, Currency> SYMBOL_TO_CURRENCY = Currency.getAvailableCurrencies().stream()
            .map(e -> Map.entry(e.getSymbol(Locale.US), e))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
