package com.budgetmaster.budgetmaster;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Currency;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

final class Util {

    static String[] splitAtWhitespace(String str, int limit) {
        return WHITESPACE_PATTERN.split(str, limit);
    }

    static String[] splitAtWhitespace(String str) {
        return WHITESPACE_PATTERN.split(str);
    }

    static Document readXml(Path xmlFile) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // never forget this!
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(xmlFile.toFile());
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException(String.format("Failed to parse %s XML file",
                    xmlFile), ex);
        }
    }

    static String appendEllipsis(String v, int maxLength) {
        return insertEllipsis(v, maxLength, EllipsisMode.APPEND);
    }

    enum EllipsisMode { APPEND, PREPEND };

    static String insertEllipsis(String v, int maxLength, EllipsisMode mode) {
        validateEllipsisStringMaxLength(maxLength);
        if (v != null && v.length() > maxLength) {
            v = v.substring(0, maxLength - ELLIPSES.length());
            switch (mode) {
                case APPEND:
                    return v + ELLIPSES;
                case PREPEND:
                    return ELLIPSES + v;
            }
        }
        return v;
    }

    static String pathEllipsis(Path v, int maxLength) {
        validateEllipsisStringMaxLength(maxLength);
        if (v == null) {
            return null;
        }
        if (v.toString().length() > maxLength) {
            String fileName = v.getFileName().toString();
            if (fileName.length() > maxLength) {
                return insertEllipsis(fileName, maxLength, EllipsisMode.PREPEND);
            }

            String dirName = appendEllipsis(v.getParent().toString(), maxLength
                    - fileName.length() - File.separator.length());
            return dirName + File.separator + fileName;
        }
        return v.toString();
    }

    private static void validateEllipsisStringMaxLength(int v) {
        if (v < ELLIPSES.length()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid max string length = %d. Should be at least %d ",
                    v, ELLIPSES.length()));
        }
    }

    final static String EOL = System.lineSeparator();

    final static Currency USD = Currency.getInstance("USD");

    private final static String ELLIPSES = "...";

    private final static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
}
