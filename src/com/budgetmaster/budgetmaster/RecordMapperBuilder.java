package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


final class RecordMapperBuilder {
    UnaryOperator<Record> createFromXml(Document doc) throws IOException {
        try {
            XPathFactory xpathfactory = XPathFactory.newInstance();
            xpath = xpathfactory.newXPath();
            globalMatchers = createRecordMatchers(doc);

            List<UnaryOperator<Record>> passes = new ArrayList<>();

            NodeList nodes = queryNodes("//pass", doc.getDocumentElement());
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                passes.add(createRecordMappers(el));
            }

            return fold(passes, true);
        } catch (XPathExpressionException ex) {
            // Should never happen at run-time.
            throw new RuntimeException(ex);
        }
    }

    UnaryOperator<Record> createFromXml(Path xmlFile) throws IOException {
        return createFromXml(Util.readXml(xmlFile));
    }

    private Map<String, Predicate<Record>> createRecordMatchers(Document doc) throws
            XPathExpressionException {
        Map<String, Predicate<Record>> result = new HashMap<>();

        NodeList nodes = queryNodes("/*/matcher[@id]", doc.getDocumentElement());
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            result.put(el.getAttribute("id"), createRecordMatcher(el));
        }

        return result;
    }

    private static UnaryOperator<Record> fold(
            List<UnaryOperator<Record>> actions, boolean iterateAll) {
        if (actions.isEmpty()) {
            return UnaryOperator.identity();
        }

        if (actions.size() == 1) {
            return actions.get(0);
        }

        if (iterateAll) {
            return (record) -> {
                Iterator<UnaryOperator<Record>> it = actions.iterator();
                while(it.hasNext() && record != null) {
                    record = it.next().apply(record);
                }
                return record;
            };
        }

        return (record) -> {
            Iterator<UnaryOperator<Record>> it = actions.iterator();
            while(it.hasNext()) {
                Record mappedRecord = it.next().apply(record);
                if (mappedRecord != record) {
                    return mappedRecord;
                }
            }
            return record;
        };
    }

    private UnaryOperator<Record> createRecordMappers(Element root) throws
            XPathExpressionException {
        List<UnaryOperator<Record>> actions = new ArrayList<>();

        NodeList nodes = queryNodes("action", root);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element)nodes.item(i);

            NodeList matcherNodes = queryNodes("matcher", el);
            for (int j = 0; j < matcherNodes.getLength(); j++) {
                Element matcherEl = (Element)matcherNodes.item(j);

                final Predicate<Record> matcher;
                if (matcherEl.hasAttribute("ref")) {
                    matcher = globalMatchers.get(matcherEl.getAttribute("ref"));
                } else {
                    matcher = createRecordMatcher(matcherEl);
                }

                UnaryOperator<Record> mapper = createRecordMapper(el);

                actions.add((record) -> {
                    if (matcher.test(record)) {
                       return mapper.apply(record);
                    }
                    return record;
                });
            }
        }

        return fold(actions, false);
    }

    private UnaryOperator<Record> createRecordMapper(Element actionEl) throws
            XPathExpressionException {
        List<UnaryOperator<Record>> actions = new ArrayList<>();

        NodeList nodes = queryNodes(
                "set-category|negate-amount|discard", actionEl);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if ("discard".equals(el.getNodeName())) {
                actions.clear();
                actions.add((record) -> null);
                break;
            }

            switch (el.getNodeName()) {
                case "negate-amount":
                    actions.add(RecordFieldMapper.amountNegator());
                    break;
                case "set-category":
                    String value = el.getFirstChild().getNodeValue();
                    actions.add(new RecordFieldMapper(value,
                            RecordBuilder::setCategory));
                    break;
                default:
                    break;
            }
        }

        return fold(actions, true);
    }

    private Predicate<Record> createRecordMatcher(Element matcherEl) throws XPathExpressionException {
        List<Predicate<Record>> predicates = new ArrayList<>();

        NodeList nodes = queryNodes("field", matcherEl);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element fieldEl = (Element)nodes.item(i);
            String fieldName = fieldEl.getAttribute("name").toLowerCase();
            Function<Record, String> fieldAccessor = FIELD_ACCESSORS.get(fieldName);
            String regexp = fieldEl.getAttribute("regexp");

            Predicate<Record> pred = new StringMatcher(regexp, fieldAccessor);
            predicates.add(pred);
        }

        nodes = queryNodes("period|period-inclusive", matcherEl);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element periodEl = (Element)nodes.item(i);

            String begin = periodEl.getAttribute("from");
            LocalDate beginDate = LocalDate.parse(begin, PERIOD_DATE_FORMAT);

            String end = periodEl.getAttribute("to");
            LocalDate endDate = LocalDate.parse(end, PERIOD_DATE_FORMAT);

            boolean inclusive = "period-inclusive".equals(periodEl.getNodeName());

            Predicate<Record> pred = new DateRangeMatcher(beginDate, endDate, inclusive);
            predicates.add(pred);
        }

        if (predicates.size() == 1) {
            return predicates.get(0);
        }

        return (record) -> {
            return predicates.stream().allMatch(pred -> pred.test(record));
        };
    }

    private NodeList queryNodes(String xpathExpr, Element root) throws
            XPathExpressionException {
        XPathExpression expr = xpath.compile(xpathExpr);
        return (NodeList) expr.evaluate(root, XPathConstants.NODESET);
    }

    private XPath xpath;
    private Map<String, Predicate<Record>> globalMatchers;

    private final static Map<String, Function<Record, String>> FIELD_ACCESSORS = Map.of(
            "description", Record::getDescription,
            "currency", (record) -> record.getCurrency().getCurrencyCode(),
            "amount", Record::getAmount,
            "category", Record::getCategory
    );

    private final static DateTimeFormatter PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "dd/MM/yy", Locale.US);
}
