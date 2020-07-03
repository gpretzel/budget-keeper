package com.budgetmaster.budgetmaster;

import com.budgetmaster.budgetmaster.Functional.ThrowingSupplier;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


final class MainRecordsProcessorBuilder {
    RecordsMapper createFromXml(Element root) throws IOException {
        XPathFactory xpathfactory = XPathFactory.newInstance();
        xpath = xpathfactory.newXPath();
        globalMatchers = createNamedRecordMatchers(root);

        final List<String> mapperIdsList;
        if (mapperIds != null) {
            mapperIdsList = List.of(mapperIds);
        } else {
            mapperIdsList = null;
        }

        List<UnaryOperator<Stream<Record>>> mappers = new ArrayList<>();

        NodeList nodes = queryNodes(Stream.of(PassType.values())
                .map(PassType::xmlName)
                .map(s -> "//" + s)
                .collect(Collectors.joining("|")), root);
        if (mapperIdsList != null) {
            Collection<String> validIDs = new HashSet<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                if (el.hasAttribute("id")) {
                    validIDs.add(el.getAttribute("id"));
                }
            }

            Set<String> requestedIDs = new HashSet<>();
            requestedIDs.addAll(mapperIdsList);
            requestedIDs.removeAll(validIDs);
            if (!requestedIDs.isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "Invalid action IDs: %s. Valid IDs: %s",
                        requestedIDs, validIDs));
            }
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);

            if (mapperIdsList != null) {
                if (!el.hasAttribute("id") || !mapperIdsList.contains(
                        el.getAttribute("id"))) {
                    continue;
                }
            }

            final String elName = el.getLocalName();
            if (elName.equals(PassType.Pass.xmlName())) {
                final UnaryOperator<Stream<Record>> mapper;
                if (el.hasAttribute("class")) {
                    String className = el.getAttribute("class");
                    var rmb = ThrowingSupplier.toSupplier(
                            () -> (PluggableSupplier<RecordsMapper>) Class.forName(
                                    className).getConstructor().newInstance()).get();
                    rmb.initFromXml(el);
                    Predicate<Record> recordMatcher = createRecordMatchers(el);

                    mapper = new RecordsMapper() {
                        @Override
                        public void onRecordsProcessed() {
                            data.onRecordsProcessed();
                        }

                        @Override
                        public Stream<Record> apply(Stream<Record> t) {
                            return data.apply(t
                                .filter(Objects::nonNull)
                                .filter(recordMatcher));
                        }

                        private final RecordsMapper data = rmb.get();
                    };
                } else {
                    UnaryOperator<Record> recordMapper = createRecordMappers(el);
                    mapper = (records) -> records
                            .filter(Objects::nonNull)
                            .map(recordMapper)
                            .filter(Objects::nonNull);
                }
                mappers.add(mapper);
            } else if (elName.equals(PassType.FilterExcludings.xmlName())) {
                Predicate<Record> recordMatcher = createRecordMatchers(el);
                ExcludingRecordsFilter filter = new ExcludingRecordsFilter();
                filter.negate(queryNodes("negate", el).getLength() != 0);
                filter.refund(queryNodes("refund", el).getLength() != 0);

                String periodDaysText = Util.readLastElement(el, "period-days");
                if (periodDaysText != null) {
                    int periodDays = Integer.parseInt(periodDaysText);
                    filter.maxPeriodDays(periodDays);
                }

                mappers.add(records -> filter.apply(records.filter(
                        Objects::nonNull).filter(recordMatcher)));
            }
        }

        return new RecordsMapper() {
            @Override
            public Stream<Record> apply(Stream<Record> t) {
                return data.apply(t);
            }

            @Override
            public void onRecordsProcessed() {
                mappers.stream()
                        .filter(RecordsMapper.class::isInstance)
                        .map(RecordsMapper.class::cast)
                        .forEachOrdered(RecordsMapper::onRecordsProcessed);
            }

            private final UnaryOperator<Stream<Record>> data = fold(mappers, true);
        };
    }

    RecordsMapper createFromXml(Path xmlFile) throws IOException {
        return createFromXml(Util.readXml(xmlFile).getDocumentElement());
    }

    MainRecordsProcessorBuilder setVaribales(Map<String, String> variables) {
        this.variables = variables;
        return this;
    }

    MainRecordsProcessorBuilder setMappersOrder(String[] mapperIds) {
        this.mapperIds = mapperIds;
        return this;
    }

    MainRecordsProcessorBuilder collectDiscardedRecords(boolean v) {
        if (v) {
            discardedRecords = Collections.synchronizedList(new ArrayList<>());
        } else {
            discardedRecords = null;
        }
        return this;
    }

    Stream<Record> getDiscardedRecords() {
        Objects.requireNonNull(discardedRecords);
        return discardedRecords.stream();
    }

    private Map<String, Predicate<Record>> createNamedRecordMatchers(
            Element root) {
        Map<String, Predicate<Record>> result = new HashMap<>();

        NodeList nodes = queryNodes("/*/matcher[@id]", root);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            result.put(el.getAttribute("id"), createRecordMatcher(el));
        }

        return result;
    }

    private static <T> UnaryOperator<T> fold(
            List<UnaryOperator<T>> actions, boolean iterateAll) {
        if (actions.isEmpty()) {
            return UnaryOperator.identity();
        }

        if (actions.size() == 1) {
            return actions.get(0);
        }

        if (iterateAll) {
            return (item) -> {
                Iterator<UnaryOperator<T>> it = actions.iterator();
                while(it.hasNext() && item != null) {
                    item = it.next().apply(item);
                }
                return item;
            };
        }

        return (item) -> {
            Iterator<UnaryOperator<T>> it = actions.iterator();
            while(it.hasNext()) {
                T mappedItem = it.next().apply(item);
                if (mappedItem != item) {
                    return mappedItem;
                }
            }
            return item;
        };
    }

    private static <T> Predicate<T> foldPredicates(List<Predicate<T>> predicates,
            boolean allMatch) {
        if (predicates.isEmpty()) {
            return s -> true;
        }

        if (predicates.size() == 1) {
            return predicates.get(0);
        }

        if (allMatch) {
            return v -> {
                return predicates.stream().allMatch(pred -> pred.test(v));
            };
        }

        return v -> {
            return predicates.stream().anyMatch(pred -> pred.test(v));
        };
    }

    private Predicate<Record> createRecordMatchers(Element root) {
        List<Predicate<Record>> matchers = new ArrayList<>();

        NodeList matcherNodes = queryNodes("matcher", root);
        for (int i = 0; i < matcherNodes.getLength(); i++) {
            Element matcherEl = (Element)matcherNodes.item(i);

            final Predicate<Record> matcher;
            if (matcherEl.hasAttribute("ref")) {
                matcher = globalMatchers.get(matcherEl.getAttribute("ref"));
            } else {
                matcher = createRecordMatcher(matcherEl);
            }
            matchers.add(matcher);
        }

        return foldPredicates(matchers, false);
    }

    private UnaryOperator<Record> createRecordMappers(Element root) {
        List<UnaryOperator<Record>> actions = new ArrayList<>();

        NodeList nodes = queryNodes("action", root);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element)nodes.item(i);

            final Predicate<Record> matcher = createRecordMatchers(el);
            final UnaryOperator<Record> mapper = createRecordMapper(el);

            actions.add((record) -> {
                if (matcher.test(record)) {
                   return mapper.apply(record);
                }
                return record;
            });
        }

        return fold(actions, false);
    }

    private static UnaryOperator<Record> loggable(UnaryOperator<Record> what,
            String msg) {
        return LoggingRecordMapper.of(what, msg);
    }

    private static Predicate<Record> loggable(Predicate<Record> what, String msg) {
        return LoggingRecordMatcher.of(what, msg);
    }

    private UnaryOperator<Record> createRecordMapper(Element actionEl) {
        List<UnaryOperator<Record>> actions = new ArrayList<>();

        NodeList nodes = queryNodes("tag|category|description|negation|discard",
                actionEl);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            switch (el.getNodeName()) {
                case "discard":
                    actions.add(LoggingRecordMapper.of((record) -> {
                        if (discardedRecords != null) {
                            discardedRecords.add(record);
                        }
                        return null;
                    }, "Discard"));
                    break;

                case "negation":
                    actions.add(loggable(RecordFieldMapper.amountNegator(),
                            "Negate amount of"));
                    break;

                case "category":
                    String value = el.getFirstChild().getNodeValue();
                    actions.add(loggable(new RecordFieldMapper(value,
                            RecordBuilder::setCategory), String.format(
                            "Set category to [%s] of", value)));
                    break;

                case "description":
                    value = el.getFirstChild().getNodeValue();
                    actions.add(loggable(new RecordFieldMapper(value,
                            RecordBuilder::setDescription), String.format(
                            "Set description to [%s] of", value)));
                    break;

                case "tag":
                    value = el.getFirstChild().getNodeValue();
                    actions.add(loggable(new RecordFieldMapper(value,
                            RecordBuilder::addTag), String.format(
                            "Add [%s] tag to", value)));
                    break;

                default:
                    break;
            }
        }

        if (nodes.getLength() == 0) {
            // Just keep the record
            actions.add(IDENTITY_ACTION);
        }

        return fold(actions, true);
    }

    private Predicate<Record> createRecordMatcher(Element root) {
        return createRecordMatcher(root, true);
    }

    private Predicate<Record> createRecordSpecificMatcher(Element el,
            boolean allMatch) {
        final String matcherType = el.getLocalName();

        if (FIELD_MATCHER_NAMES.contains(matcherType)) {
            Function<Record, String> fieldAccessor = FIELD_ACCESSORS.get(
                    matcherType);
            String regexp = el.getFirstChild().getNodeValue();

            return loggable(new StringMatcher(regexp, fieldAccessor),
                    String.format("Regexp [%s] applied to %s", regexp,
                            matcherType));
        }

        switch (matcherType) {
            case "not":
                return createRecordMatcher(el, allMatch).negate();

            case "or":
                return createRecordMatcher(el, false);

            case "matcher":
                return globalMatchers.get(el.getAttribute("ref"));

            case "tag": {
                String tag = el.getFirstChild().getNodeValue();
                return loggable((Predicate<Record>) (record) -> record.hasTag(
                        tag), String.format("Tag [%s]", tag));
            }

            case "period-inclusive":
            case "period": {
                String begin = el.getAttribute("from");
                LocalDate beginDate = LocalDate.parse(begin, PERIOD_DATE_FORMAT);

                String end = el.getAttribute("to");
                LocalDate endDate = LocalDate.parse(end, PERIOD_DATE_FORMAT);

                boolean inclusive = "period-inclusive".equals(el.getNodeName());

                final String openBraket, closeBraket;
                if (inclusive) {
                    openBraket = "[";
                    closeBraket = "]";
                } else {
                    openBraket = "(";
                    closeBraket = ")";
                }

                return loggable(new DateRangeMatcher(beginDate, endDate,
                        Record::getTransactionDate, inclusive), String.format(
                        "Date range %s%s-%s%s", openBraket, begin, end,
                        closeBraket));
            }
        }

        throw new IllegalArgumentException();
    }

    private Predicate<Record> createRecordMatcher(Element root, boolean allMatch) {
        List<Predicate<Record>> predicates = new ArrayList<>();

        NodeList nodes = queryNodes(SPECIFIC_MATCHER_XPATH, root);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element)nodes.item(i);
            predicates.add(createRecordSpecificMatcher(el, allMatch));
        }

        return foldPredicates(predicates, allMatch);
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
    private Map<String, Predicate<Record>> globalMatchers;
    private List<Record> discardedRecords;
    private String[] mapperIds;
    private Map<String, String> variables;

    private final static Map<String, Function<Record, String>> FIELD_ACCESSORS = Map.of(
            "description", Record::getDescription,
            "currency", Record::getCurrencyCode,
            "category", Record::getCategory,
            "statement-id", (record) -> record.getSource().getId()
    );

    private final static Set<String> FIELD_MATCHER_NAMES = FIELD_ACCESSORS.keySet();

    private final static String SPECIFIC_MATCHER_XPATH = Stream.concat(Stream.of("or",
            "not", "matcher[@ref]", "tag", "period", "period-inclusive"),
            FIELD_MATCHER_NAMES.stream()).collect(Collectors.joining("|"));

    private final static DateTimeFormatter PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "dd/MM/yy", Locale.US);

    private final static UnaryOperator<Record> IDENTITY_ACTION = LoggingRecordMapper.of(
            (record) -> RecordBuilder.from(record).create(), "Identity");

    enum PassType {
        Pass,
        FilterExcludings;

        public String xmlName() {
            return String.join("-", name().split("(?=\\p{Lu})")).toLowerCase();
        }
    };
}
