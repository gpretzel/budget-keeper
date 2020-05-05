package com.budgetmaster.budgetmaster;

import static com.budgetmaster.budgetmaster.Util.EOL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        // trip unneeded trailing characters.
        String transactionsText = rawTransactionsText.split(EOL + "Fees" + EOL, 2)[0];
        
        result.addAll(parseTransactionTable(transactionsText));

        if (transactionsText.length() == rawTransactionsText.length()) {
            // Transactions table continued on the second page 
            // FIXME: What if transactions table is split in more than two chunks
            try {
                transactionsText = text.set(EOL + "Date Date Advances Credits" + EOL,
                        EOL + "Fees" + EOL).getStrippedValue();
            } catch (TextFrameException ex) {
                return result;
            }

            result.addAll(parseTransactionTable(transactionsText));
        }
        
        return result;
    }
    
    private List<Record> parseTransactionTable(String text) {
        String lines[] = RECORD_SPLITTER.split(text);
            
        Stream recordStream = Stream.of(lines)
                .filter((str) -> {
                    Matcher m = TRANSACTION_HEADER.matcher(str);
                    return m.matches();
                });
        
        return parseRecords(recordStream);
    }

    @Override
    protected void parseRecord(String str, RecordBuilder rb) {
        // 12/12 GULF OIL 91186030 SOMERVILLE MA 20.79
        String[] components = Util.splitAtWhitespace(str);
        
        rb.setAmount(components[components.length - 1]);
        String desc = Stream.of(components)
                .skip(1)
                .limit(components.length - 2)
                .collect(Collectors.joining(" "));
        rb.setDescription(desc);
    }
    
    @Override
    protected String periodString(TextFrame text) {
        // Extract from
        // Statement Closing Date 01/10/18
        String result = text.set(EOL + "Statement Closing Date", EOL).getStrippedValue();
        return result;
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
    
    private final static DateTimeFormatter TRANSACTION_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MM/dd", Locale.US);

    private final static DateTimeFormatter STATEMENT_PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MM/dd/yy", Locale.US);
    
    private final static Pattern RECORD_SPLITTER = Pattern.compile("\\R");
    
    private final static Pattern TRANSACTION_HEADER = Pattern.compile(
            "^\\d{2}/\\d{2}\\s+\\d{2}/\\d{2}.+$");
}
