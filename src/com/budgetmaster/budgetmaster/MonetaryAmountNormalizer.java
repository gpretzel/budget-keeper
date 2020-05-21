package com.budgetmaster.budgetmaster;


public class MonetaryAmountNormalizer {
    public String normalize(String value) {
        String result = value.strip();
        int sepIdx = result.indexOf(',');
        if (sepIdx > 0) {
            result = result.substring(0, sepIdx) + result.substring(sepIdx + 1);
        }

        if (negate) {
            if (result.charAt(0) == '-') {
                result = result.substring(1);
            } else {
                result = "-" + result;
            }
        }

        return result;
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
