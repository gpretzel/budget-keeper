package com.budgetmaster.budgetmaster;

import static com.budgetmaster.budgetmaster.Util.EOL;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TdBankStatementPdf extends AccountStatementPdf {
    @Override
    protected List<Record> parsePdfText(TextFrame text) {
        BalanceChecksum balanceChecksum = createBalanceChecksum(text);

        text = new TextFrame(EOL + text.set("DAILY ACCOUNT ACTIVITY",
                "DAILY BALANCE SUMMARY").getValue());

        List<Record> result = new ArrayList<>();

        int recordCount;
        continuedOnTheNextPage = false;
        do {
            if (continuedOnTheNextPage) {
                text.set("DAILY ACCOUNT ACTIVITY", EOL);
                continuedOnTheNextPage = false;
            }

            recordCount = result.size();
            negateCurrentRecordAmount = true;
            result.addAll(parseDailyAccountActivity(text, "Deposits"));
            if (continuedOnTheNextPage) {
                continue;
            }

            result.addAll(parseDailyAccountActivity(text, "Electronic Deposits"));
            if (continuedOnTheNextPage) {
                continue;
            }

            negateCurrentRecordAmount = false;
            result.addAll(parseDailyAccountActivity(text, "Checks Paid")
                    .stream()
                    .map(RecordBuilder::from)
                    .peek((rb) -> rb.setDescription("Check #" + rb.getDescription()))
                    .map(RecordBuilder::create)
                    .collect(Collectors.toList()));
            if (continuedOnTheNextPage) {
                continue;
            }

            result.addAll(parseDailyAccountActivity(text, "Electronic Payments"));
            if (continuedOnTheNextPage) {
                continue;
            }

            result.addAll(parseDailyAccountActivity(text, "Service Charges"));
            if (continuedOnTheNextPage) {
                continue;
            }
        } while (recordCount != result.size());

        balanceChecksum.append(result.stream()).validate();

        return result;
    }

    private static BalanceChecksum createBalanceChecksum(TextFrame text) {
        BigDecimal beginBalance = MonetaryAmount.of(text.set(EOL
                + "Beginning Balance", EOL).getStrippedValue());
        BigDecimal endBalance = MonetaryAmount.of(text.set(EOL
                + "Ending Balance", EOL).getStrippedValue());

        return new BalanceChecksum(beginBalance.subtract(endBalance));
    }

    @Override
    protected void parseRecord(String str, RecordBuilder rb) {
        int amountIdx = str.lastIndexOf(' ');
        String amountValue = str.substring(amountIdx).strip();
        if (negateCurrentRecordAmount) {
            amountValue = "-" + amountValue;
        }
        rb.setAmount(amountValue);
        rb.setDescription(str.substring(0, amountIdx).strip());
    }

    @Override
    protected String periodString(TextFrame text) {
        // Extract dates substring from
        // 'Statement Period: Dec 06 2018-Jan 05 2019' string
        return text.set("Statement Period:", EOL).getStrippedValue();
    }

    @Override
    protected String periodStringSeparator() {
        return "-";
    }

    @Override
    protected DateTimeFormatter periodStringDateTimeFormatter() {
        return STATEMENT_PERIOD_DATE_FORMAT;
    }

    @Override
    protected DateTimeFormatter recordDateTimeFormatter() {
        return TRANSACTION_DATE_FORMAT;
    }

    private List<Record> parseDailyAccountActivity(TextFrame text, String header) {
        String subText;
        try {
            subText = text.set(EOL + header, EOL + "Subtotal").getStrippedValue();
            int continuedOnTheNextPageIdx = subText.indexOf(EOL
                    + "How to Balance your Account");
            if (continuedOnTheNextPageIdx != -1) {
                subText = subText.substring(0, continuedOnTheNextPageIdx);
                continuedOnTheNextPage = true;
            }
        } catch (TextFrameException ex) {
            return Collections.emptyList();
        }

        String lines[] = RECORD_SPLITTER.split(subText);

        // skip "POSTING DATE DESCRIPTION AMOUNT" header row
        Stream recordStream = Stream.of(lines).skip(1);
        return parseRecords(recordStream);
    }

    private boolean negateCurrentRecordAmount;
    private boolean continuedOnTheNextPage;

    private final static DateTimeFormatter TRANSACTION_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MM/dd");

    private final static DateTimeFormatter STATEMENT_PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MMM dd yyyy", Locale.US);

    private final static Pattern RECORD_SPLITTER = Pattern.compile(
            "\\R(?=\\d{2}/\\d{2})");
}
