package com.budgetmaster.budgetmaster;

import java.math.BigDecimal;


public class MonetaryAmountNormalizer {
    public String normalize(String value) {
        String result = value.strip();
        int sepIdx = result.indexOf(',');
        if (sepIdx > 0) {
            result = result.substring(0, sepIdx) + result.substring(sepIdx + 1);
        }

        BigDecimal number = new BigDecimal(result).stripTrailingZeros();
        if (negate) {
            number = number.negate();
        }

        return number.toPlainString();
    }

    public static MonetaryAmountNormalizer negate(boolean v) {
        return new MonetaryAmountNormalizer().setNegate(v);
    }

    public static MonetaryAmountNormalizer negate() {
        return negate(true);
    }

    public MonetaryAmountNormalizer setNegate(boolean v) {
        negate = v;
        return this;
    }

    private boolean negate;
}
