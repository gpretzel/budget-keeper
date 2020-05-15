package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


abstract class AccountStatementPdf implements RecordSupplier {
    @Override
    public Stream<Record> read(Path pdfFile) throws IOException {
        PdfTextExtractor textExtractor = new PdfTextExtractor();
        adjustPdfTextExtractor(textExtractor);
        String pdfText = textExtractor.extractText(pdfFile);

        TextFrame textFrame = new TextFrame(pdfText);
        setPeriod(textFrame);

        return parsePdfText(textFrame).stream();
    }
    
    protected List<Record> parseRecords(Stream<String> lines) {
        return lines
                .map((str) -> str.replaceAll("[\\n\\r\\s]+", " "))
                .map(this::parseRecord)
                .collect(Collectors.toList());
    }

    private Record parseRecord(String str) {
        RecordBuilder rb = new RecordBuilder().strip();
        
        String components[] = Util.splitAtWhitespace(str, 2);
        
        rb.setDate(getRecordDate(components[0]));
        
        parseRecord(components[1], rb);
        
        return rb.create();
    }
            
    private LocalDate getRecordDate(String str) {
        try {
            MonthDay md = MonthDay.parse(str, recordDateTimeFormatter());
            return year.atMonthDay(md);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private void setPeriod(TextFrame text) {
        String periodStr = periodString(text);

        String components[];
        if (periodStringSeparator() != null) {
            components = periodStr.split(periodStringSeparator(), 2);
        } else {
            components = new String[] { periodStr };
        }

        try {
            LocalDate date = LocalDate.parse(components[0],
                    periodStringDateTimeFormatter());
            year = Year.from(date);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(String.format(
                    "Can't parse statement period start date from [%s] string",
                    periodStr), ex);
        }
    }
    
    protected void adjustPdfTextExtractor(PdfTextExtractor extractor) {
    }
    
    protected abstract List<Record> parsePdfText(TextFrame text);
    
    protected abstract void parseRecord(String str, RecordBuilder rb);
    
    protected abstract String periodString(TextFrame text);
    
    protected abstract String periodStringSeparator();
    
    protected abstract DateTimeFormatter periodStringDateTimeFormatter();
    
    protected abstract DateTimeFormatter recordDateTimeFormatter();

    private Year year;
}
