package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Currency;
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

    final static String EOL = System.lineSeparator();

    final static Currency USD = Currency.getInstance("USD");

    private final static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
}
