package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class RecordsSerializer {
    static RecordsSerializer csv() {
        RecordsSerializer result = new RecordsSerializer();

        String fields[] = new String[] {
            "{DATE}", "{AMOUNT}", "{CURRENCY}", "{CATEGORY}", "{DESC}", "{SCLASS}", "{SID}"
        };
        result.setRecordFormat(
                Stream.of(fields).map((s) -> String.format("\"%s\"", s)).collect(
                        Collectors.joining(",")));

        return result;
    }
    
    RecordsSerializer() {
        setDateFormat("dd/MM/yy");
    }

    void saveToFile(Stream<Record> records, Path csvOutputFile) throws IOException {
        Stream<String> lines = records.map(this::stringify);
        if (header != null) {
            lines = Stream.of(Stream.of(header), lines).flatMap(s -> s);
        }
        if (footer != null) {
            lines = Stream.of(Stream.of(footer), lines).flatMap(s -> s);
        }
        Files.write(csvOutputFile, lines.collect(Collectors.toList()));
    }

    RecordsSerializer withCsvHeader() {
        // Build header line from ordered field names.
        setHeader(Stream.of(getOrderedFields())
                .map(Field::name)
                .collect(Collectors.joining(",")));
        return this;
    }
    
    DateTimeFormatter getDateTimeFormatter() {
        return dateFormat;
    }
    
    Field[] getOrderedFields() {
        if (recordFormatMap == null) {
            throw new IllegalStateException();
        }
        
        return recordFormatMap.entrySet().stream()
                .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(e -> e.getKey())
                .toArray(Field[]::new);
    }

    RecordsSerializer setHeader(String header) {
        this.header = header;
        return this;
    }
    
    RecordsSerializer setFooter(String footer) {
        this.footer = footer;
        return this;
    }
    
    RecordsSerializer setDateFormat(String formatStr, String localeStr) {
        final Locale locale;
        if (localeStr == null) {
            locale = Locale.US;
        } else {
            locale = Locale.forLanguageTag(localeStr);
        }
        dateFormat = DateTimeFormatter.ofPattern(formatStr, locale);
        return this;
    }

    RecordsSerializer setDateFormat(String formatStr) {
        return setDateFormat(formatStr, null);
    }

    RecordsSerializer setRecordFormat(String formatStr) {
        recordFormatMap = new HashMap<>();

        Matcher matcher = FIELD_PATTERN.matcher(formatStr);
        List<String> components = new ArrayList<>();
        int start = 0;
        while (matcher.find()) {
            final int newStart = matcher.start();
            final int newEnd = matcher.end();
            final String substr = formatStr.substring(start, newStart);
            if (!substr.isEmpty()) {
                components.add(substr);
            }
            start = newEnd;
            final String field = formatStr.substring(newStart, newEnd);
            recordFormatMap.put(FIELD_FORMAT_MAP.get(field), components.size());
            components.add(null);
        }
        if (start != formatStr.length()) {
            components.add(formatStr.substring(start));
        }

        recordFormat = components.toArray(String[]::new);
        return this;
    }

    private void setFieldValue(String[] recordFields, Field field,
            Supplier<String> valueSupplier) {
        if (recordFormatMap.containsKey(field)) {
            String value = valueSupplier.get();
            if (value != null) {
                value = String.format("%s", value);
            } else {
                value = "";
            }
            recordFields[recordFormatMap.get(field)] = value;
        }
    }

    private String stringify(Record record) {
        final String[] recordFields = Arrays.copyOf(recordFormat,
                recordFormat.length);

        setFieldValue(recordFields, Field.Date, () -> record.getDate().format(
                dateFormat));
        setFieldValue(recordFields, Field.Amount, () -> record.getAmount());
        setFieldValue(recordFields, Field.Category, () -> record.getCategory());
        setFieldValue(recordFields, Field.Currency, () -> record.getCurrencyCode());
        setFieldValue(recordFields, Field.Description,
                () -> record.getDescription());
        setFieldValue(recordFields, Field.StatementId, () -> record.getSource()
                == null ? null : record.getSource().getId());
        setFieldValue(recordFields, Field.StatementSystemId,
                () -> record.getSource() == null ? null : record.getSource().getSystemId());

        return String.join("", recordFields);
    }

    enum Field {
        Date,
        Amount,
        Currency,
        Category,
        Description("DESC"),
        StatementId("SCLASS"),
        StatementSystemId("SID");

        Field() {
            tag = name().toUpperCase();
        }

        Field(String tag) {
            this.tag = tag;
        }

        final String tag;
    };

    private DateTimeFormatter dateFormat;
    private String[] recordFormat;
    private String header;
    private String footer;
    private Map<Field, Integer> recordFormatMap;

    private final static Pattern FIELD_PATTERN = Pattern.compile(String.format(
            "\\{(%s)\\}", Stream.of(Field.values()).map((v) -> v.tag).collect(
                    Collectors.joining("|"))));

    private Map<String, Field> FIELD_FORMAT_MAP = Stream.of(Field.values()).collect(
            Collectors.toMap((v) -> String.format("{%s}", v.tag), (v) -> v));
}
