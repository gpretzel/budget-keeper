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
        try {
            XPathFactory xpathfactory = XPathFactory.newInstance();
            xpath = xpathfactory.newXPath();

            List<Function<Path, Statement>> parsers = new ArrayList<>();

            NodeList nodes = queryNodes("//parser", doc.getDocumentElement());
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                parsers.add(createRecordSupplier(el));
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
        } catch (XPathExpressionException ex) {
            // Should never happen at run-time.
            throw new RuntimeException(ex);
        }
    }

    private static RecordSupplier createRecordSupplier(String clazz) {
        try {
            return (RecordSupplier) Class.forName(clazz).getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException
                | ClassNotFoundException | NoSuchMethodException
                | SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    private Function<Path, Statement> createRecordSupplier(Element parserEl)
            throws XPathExpressionException {

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

        RecordSupplier rs = createRecordSupplier(clazz);

        return (path) -> {
            for (Predicate<Path> m: matchers) {
               if (m.test(path)) {
                   return Statement.fromStatementFile(id, path, currency, rs);
               }
            }
            return null;
        };
    }

    Function<Path, Statement> createFromXml(Path xmlFile)
            throws IOException {
        return createFromXml(Util.readXml(xmlFile));
    }

    private NodeList queryNodes(String xpathExpr, Element root) throws
            XPathExpressionException {
        XPathExpression expr = xpath.compile(xpathExpr);
        return (NodeList) expr.evaluate(root, XPathConstants.NODESET);
    }

    private XPath xpath;
}
