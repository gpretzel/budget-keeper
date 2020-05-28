package com.budgetmaster.budgetmaster;

import static com.budgetmaster.budgetmaster.Util.EOL;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DcuStatementPdf extends AccountStatementPdf {
    @Override
    protected void adjustPdfTextExtractor(PdfTextExtractor extractor) {
        extractor.setSortByPosition(true);
    }

    @Override
    protected List<Record> parsePdfText(TextFrame text) {
        List<Record> result = new ArrayList<>();

        while (true) {
            BalanceChecksum accountChecksum;
            String accountText;
            try {
                text.set("ACCT#", "BALANCE");

                final String beginBalanceStr = text.set(EOL + "PREVIOUS BALANCE", EOL).getStrippedValue();
                final BigDecimal beginBalance;
                if (beginBalanceStr.isEmpty()) {
                    beginBalance = BigDecimal.ZERO;
                } else {
                    beginBalance = new BigDecimal(
                        MonetaryAmountNormalizer.negate().normalize(beginBalanceStr));
                }

                accountText = text.set(EOL, "NEW BALANCE").getStrippedValue();

                final BigDecimal endBalance = new BigDecimal(
                        MonetaryAmountNormalizer.negate().normalize(text.set(
                                "NEW BALANCE", EOL).getStrippedValue()));

                accountChecksum = new BalanceChecksum(endBalance.subtract(
                        beginBalance));

            } catch (TextFrameException ex) {
                break;
            }

            String lines[] = RECORD_SPLITTER.split(accountText);

            Stream recordStream = Stream.of(lines)
                    .filter((str) -> {
                        Matcher m = TRANSACTION_PATTERN.matcher(str);
                        return m.matches();
                    });

            int skipRecordsCount = result.size();

            result.addAll(parseRecords(recordStream));

            accountChecksum.append(result.stream().skip(skipRecordsCount)).validate();
        }

        return result;
    }

    @Override
    protected void parseRecord(String str, RecordBuilder rb) {
        // HR batch credits   VisaCashBack              180110 9.06 7,243.79
        String[] components = Util.splitAtWhitespace(str);

        rb.negateAmount().setAmount(components[components.length - 2]);
        String desc = Stream.of(components)
                .limit(components.length - 2)
                .collect(Collectors.joining(" "));
        rb.setDescription(desc);
    }

    @Override
    protected String periodString(TextFrame text) {
        // Extract dates substring from
        // 'BANKING – THE DCU WAY 5891321 01-01-18 to 01-31-18 1 of 2' string
        String result = text.set("BANKING – THE DCU WAY", EOL).getStrippedValue();
        String[] components = Util.splitAtWhitespace(result);
        return String.join(" ", components[1], components[2], components[3]);
    }

    @Override
    protected String periodStringSeparator() {
        return " to ";
    }

    @Override
    protected DateTimeFormatter periodStringDateTimeFormatter() {
        return STATEMENT_PERIOD_DATE_FORMAT;
    }

    @Override
    protected DateTimeFormatter recordDateTimeFormatter() {
        return TRANSACTION_DATE_FORMAT;
    }

    private final static DateTimeFormatter TRANSACTION_DATE_FORMAT =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(DateTimeFormatter.ofPattern("MMMdd", Locale.US))
                    .toFormatter();

    private final static DateTimeFormatter STATEMENT_PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MM-dd-yy", Locale.US);

    private final static Pattern RECORD_SPLITTER = Pattern.compile("\\R");

    private final static Pattern TRANSACTION_PATTERN = Pattern.compile(
            "^[A-Z]{3}\\d{2}.+\\s+[\\d-.,]+\\s+[\\d-.,]+$");
}
