package com.budgetmaster.budgetmaster;

import java.util.regex.Pattern;

final class Util {
    
    static String[] splitAtWhitespace(String str, int limit) {
        return WHITESPACE_PATTERN.split(str, limit);
    }
    
    static String[] splitAtWhitespace(String str) {
        return WHITESPACE_PATTERN.split(str);
    }
    
    final static String EOL = System.lineSeparator();

    private final static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
}
