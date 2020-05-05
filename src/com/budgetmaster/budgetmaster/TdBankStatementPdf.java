package com.budgetmaster.budgetmaster;

import static com.budgetmaster.budgetmaster.Util.EOL;
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
        text = new TextFrame(EOL + text.set(
                "DAILY ACCOUNT ACTIVITY", "DAILY BALANCE SUMMARY").getValue());
        
        List<Record> result = new ArrayList<>();

        result.addAll(
                parseDailyAccountActivity(text, "Electronic Deposits")
                        .stream()
                        .map(RecordBuilder::from)
                        .map(RecordBuilder::negateAmount)
                        .map(RecordBuilder::create)
                        .collect(Collectors.toList()));
        result.addAll(parseDailyAccountActivity(text, "Checks Paid")
                .stream()
                .map(RecordBuilder::from)
                .peek((rb) -> rb.setDescription("Check #" + rb.getDescription()))
                .map(RecordBuilder::create)
                .collect(Collectors.toList()));
        result.addAll(parseDailyAccountActivity(text, "Electronic Payments"));

        return result;
    }
    
    @Override
    protected void parseRecord(String str, RecordBuilder rb) {
        int amountIdx = str.lastIndexOf(' ');
        rb.setAmount(str.substring(amountIdx).strip());
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
        } catch (TextFrameException ex) {
            return Collections.emptyList();
        }
        
        String lines[] = RECORD_SPLITTER.split(subText);
        
        // skip "POSTING DATE DESCRIPTION AMOUNT" header row
        Stream recordStream = Stream.of(lines).skip(1);
        return parseRecords(recordStream);
    }

    private final static DateTimeFormatter TRANSACTION_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MM/dd");

    private final static DateTimeFormatter STATEMENT_PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MMM dd yyyy", Locale.US);
    
    private final static Pattern RECORD_SPLITTER = Pattern.compile(
            "\\R(?=\\d{2}/\\d{2})");
}
