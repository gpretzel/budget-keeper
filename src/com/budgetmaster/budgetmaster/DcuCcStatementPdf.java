package com.budgetmaster.budgetmaster;

import static com.budgetmaster.budgetmaster.Util.EOL;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DcuCcStatementPdf extends AccountStatementPdf {
    @Override
    protected void adjustPdfTextExtractor(PdfTextExtractor extractor) {
        extractor.setSortByPosition(false);
    }

    @Override
    protected List<Record> parsePdfText(TextFrame text) {
        List<Record> result = new ArrayList<>();

        // Get the first page transactions table
        String rawTransactionsText = text.set(EOL + "Date Date Advances Credits" + EOL,
                EOL + "*PERIODIC FINANCE CHARGE (*)" + EOL).getStrippedValue();

        // In case transactions table is not split accross the pages
        // strip unneeded trailing characters.
        String transactionsText = rawTransactionsText.split(EOL + "Fees" + EOL, 2)[0];

        if (transactionsText.length() == rawTransactionsText.length()) {
            // Transactions table continued on the second page
            // FIXME: What if transactions table is split in more than two chunks
            try {
                String moreTransactionsText = text.set(EOL + "Date Date Advances Credits" + EOL,
                        EOL + "Fees" + EOL).getStrippedValue();
                transactionsText = transactionsText + EOL + moreTransactionsText;
            } catch (TextFrameException ex) {
            }
        }

        result.addAll(parseTransactionTable(transactionsText));

        paymentsChecksum.append(result.stream().filter(Record::isNegative)).validate();
        purchasesChecksum.append(result.stream().filter(Predicate.not(
                Record::isNegative))).validate();

        return result;
    }

    private List<Record> parseTransactionTable(String text) {
        String rawLines[] = RECORD_SPLITTER.split(text);

        List<String> lines = new ArrayList<>();

        for (String line: rawLines) {
            Matcher m = TRANSACTION_HEADER.matcher(line);
            if (m.matches()) {
                lines.add(line);
            } else if (!lines.isEmpty()) {
                String prevLine = lines.get(lines.size() - 1);
                int idx = prevLine.lastIndexOf(' ');
                line = prevLine.substring(0, idx) + " " + line + prevLine.substring(idx);
                lines.set(lines.size() - 1, line);
            }
        }

        return parseRecords(lines.stream());
    }

    @Override
    protected void parseRecord(String str, RecordBuilder rb) {
        // 12/12 GULF OIL 91186030 SOMERVILLE MA 20.79
        String[] components = Util.splitAtWhitespace(str);

        rb.setPostingDate(getRecordDate(components[0]));

        rb.setAmount(components[components.length - 1]);
        String desc = Stream.of(components)
                .skip(1)
                .limit(components.length - 2)
                .collect(Collectors.joining(" "));
        rb.setDescription(desc);

        if (PAYMENT_TRANSACTION.matcher(desc).matches()) {
            rb.negateAmount();
        }
    }

    @Override
    protected String periodString(TextFrame text) {
        text.set(EOL + "Starting Balance", EOL);

        paymentsChecksum = createRecordChecksum(text, "Payments",
                "Other Credits");

        // Extract from
        // Statement Closing Date 01/10/18
        String endingPeriodStr = text.set(EOL + "Statement Closing Date", EOL).getStrippedValue();

        purchasesChecksum = createRecordChecksum(text, "Purchases", null);

        String daysInPeriodStr = text.set(EOL + "Days in Period", EOL).getStrippedValue();

        try {
            LocalDate endingPeriod = LocalDate.parse(endingPeriodStr, periodStringDateTimeFormatter());
            int daysInPeriod = Integer.parseInt(daysInPeriodStr);

            String result = String.join(periodStringSeparator(),
                    endingPeriod.minusDays(daysInPeriod + 1).format(
                            periodStringDateTimeFormatter()), endingPeriodStr);
            return result;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(String.format(
                    "Can't parse statement period end date from [%s] string",
                    endingPeriodStr), ex);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(String.format(
                    "Can't parse statement days in period [%s] string",
                    daysInPeriodStr), ex);
        }
    }

    private static BalanceChecksum createRecordChecksum(TextFrame text, String main, String other) {
        final BigDecimal mainAmount = getChecksumValue(text, EOL + main);

        LOGGER.finest(String.format("%s amount: [%s]", main, mainAmount));

        final BigDecimal totalAmount;

        if (other != null) {
            final BigDecimal otherAmount = getChecksumValue(text, EOL + other);
            if (otherAmount.compareTo(BigDecimal.ZERO) != 0) {
                LOGGER.finest(String.format("%s amount: [%s]", other, otherAmount));
                totalAmount = mainAmount.add(otherAmount);
            } else {
                totalAmount = mainAmount;
            }
        } else {
            totalAmount = mainAmount;
        }

        return new BalanceChecksum(totalAmount);
    }

    private static BigDecimal getChecksumValue(TextFrame text, String begin) {
        String checksumText = text.set(begin, EOL).getStrippedValue();
        return MonetaryAmount.of(checksumText);
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

    private BalanceChecksum paymentsChecksum;
    private BalanceChecksum purchasesChecksum;

    private static final Logger LOGGER = Logger.getLogger(
            MethodHandles.lookup().lookupClass().getName());

    private final static DateTimeFormatter TRANSACTION_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MM/dd", Locale.US);

    private final static DateTimeFormatter STATEMENT_PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MM/dd/yy", Locale.US);

    private final static Pattern RECORD_SPLITTER = Pattern.compile("\\R");

    private final static Pattern TRANSACTION_HEADER = Pattern.compile(
            "^\\d{2}/\\d{2}\\s+\\d{2}/\\d{2}.+$");

    private final static Pattern PAYMENT_TRANSACTION = Pattern.compile(
            "^.*\\b(PAYMENT-TRANSFER|PAYMENT-PAYOFF|CREDIT\\s+CARD\\s+CREDIT)\\b.*$");
}
