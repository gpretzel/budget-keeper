package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class StatementReaderBuilder {
    Function<Path, Statement> createFromXml(Document doc) throws IOException {
        XPathFactory xpathfactory = XPathFactory.newInstance();
        xpath = xpathfactory.newXPath();

        List<Function<Path, Statement>> parsers = new ArrayList<>();

        NodeList nodes = queryNodes("//parser", doc.getDocumentElement());
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            parsers.add(createRecordsSupplier(el));
        }

        return (path) -> {
            Statement result = null;
            for (Function<Path, Statement> parser: parsers) {
               result = parser.apply(path);
               if (result != null) {
                   break;
               }
            }
            return result;
        };
    }

    private static <T> T newInstance(Class clazz) {
        try {
            return (T) clazz.getConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException
                | NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    private static PluggableSupplier<RecordsSupplier> createRecordsSupplierFactory(
            String className) {
        try {
            Class clazz = Class.forName(className);
            if (PluggableSupplier.class.isAssignableFrom(clazz)) {
                return newInstance(clazz);
            }
            return () -> newInstance(clazz);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Function<Path, Statement> createRecordsSupplier(Element parserEl) {

        List<Predicate<Path>> matchers = new ArrayList<>();

        String id = parserEl.getAttribute("id");
        String clazz = parserEl.getAttribute("class");

        final Currency currency;
        if (parserEl.hasAttribute("currency")) {
            currency = Currency.getInstance(parserEl.getAttribute("currency"));
        } else {
            currency = null;
        }

        NodeList nodes = queryNodes("path-matcher", parserEl);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element matcherEl = (Element)nodes.item(i);

            String value = matcherEl.getFirstChild().getNodeValue();

            PathMatcher pm = FileSystems.getDefault().getPathMatcher(value);

            matchers.add(pm::matches);
        }

        var rsf = createRecordsSupplierFactory(clazz);
        rsf.initFromXml(parserEl);

        return (path) -> {
            for (Predicate<Path> m : matchers) {
                if (m.test(path)) {
                    return Statement.fromStatementFile(id, path, currency,
                            rsf.get());
                }
            }
            return null;
        };
    }

    Function<Path, Statement> createFromXml(Path xmlFile)
            throws IOException {
        return createFromXml(Util.readXml(xmlFile));
    }

    private NodeList queryNodes(String xpathExpr, Element root) {
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            return (NodeList) expr.evaluate(root, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            throw new RuntimeException(ex);
        }
    }

    private XPath xpath;
}
