package com.budgetmaster.budgetmaster;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Currency;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class Util {

    public static String[] splitAtWhitespace(String str, int limit) {
        return WHITESPACE_PATTERN.split(str, limit);
    }

    public static String[] splitAtWhitespace(String str) {
        return WHITESPACE_PATTERN.split(str);
    }

    public static Document readXml(Path xmlFile) throws IOException {
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

    public static String appendEllipsis(String v, int maxLength) {
        return insertEllipsis(v, maxLength, EllipsisMode.APPEND);
    }

    enum EllipsisMode { APPEND, PREPEND };

    public static String insertEllipsis(String v, int maxLength, EllipsisMode mode) {
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

    public static String pathEllipsis(Path v, int maxLength) {
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

    public static void saveToCsvFile(Path path, Stream<Record> records) throws
            IOException {
        RecordsSerializer serializer = RecordsSerializer.csv().withCsvHeader();
        if ("--".equals(path.toString())) {
            serializer.saveToStream(records, System.out);
        } else {
            serializer.saveToFile(records, path);
        }
    }

    public static String readLastElement(Element root, String elementName) {
        XPathFactory xpathfactory = XPathFactory.newInstance();
        XPath xpath = xpathfactory.newXPath();

        try {
            XPathExpression expr = xpath.compile(elementName + "[last()]/text()");
            NodeList nodes = (NodeList) expr.evaluate(root, XPathConstants.NODESET);
            if (nodes.getLength() != 0) {
                return nodes.item(0).getNodeValue();
            }
            return null;
        } catch (XPathExpressionException ex) {
            // Should never happen
            throw new RuntimeException(ex);
        }
    }

    private static void validateEllipsisStringMaxLength(int v) {
        if (v < ELLIPSES.length()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid max string length = %d. Should be at least %d ",
                    v, ELLIPSES.length()));
        }
    }

    public final static String EOL = System.lineSeparator();

    public final static Currency USD = Currency.getInstance("USD");

    private final static String ELLIPSES = "...";

    private final static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
}
