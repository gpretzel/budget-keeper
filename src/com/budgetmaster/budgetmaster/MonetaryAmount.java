package com.budgetmaster.budgetmaster;

import java.math.BigDecimal;


public final class MonetaryAmount {
    public static BigDecimal of(String value) {
        String result = value.strip();
        int sepIdx = result.indexOf(',');
        if (sepIdx > 0) {
            result = result.substring(0, sepIdx) + result.substring(sepIdx + 1);
        }

        return new BigDecimal(result).stripTrailingZeros();
    }
}
