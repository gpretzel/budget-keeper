package com.budgetmaster.budgetmaster;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class AccountStatementPdf implements RecordSupplier {
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

        rb.setTransactionDate(getRecordDate(components[0]));

        parseRecord(components[1], rb);

        return rb.create();
    }

    protected LocalDate getRecordDate(String str) {
        try {
            MonthDay md = MonthDay.parse(str, recordDateTimeFormatter());
            LocalDate date = Year.from(beginPeriod).atMonthDay(md);
            if (date.isBefore(beginPeriod)) {
                date = Year.from(endPeriod).atMonthDay(md);
                if (date.isAfter(endPeriod)) {
                    throw new IllegalArgumentException(String.format(
                            "Month day [%s] is out of statement period [%s - %s]",
                            md.format(MONTH_DAY_DATE_FORMAT), beginPeriod, endPeriod));
                }
            }
            return date;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private void setPeriod(TextFrame text) {
        String periodStr = periodString(text);

        String components[] = periodStr.split(periodStringSeparator(), 2);

        try {
            beginPeriod = LocalDate.parse(components[0],
                    periodStringDateTimeFormatter());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(String.format(
                    "Can't parse statement period start date from [%s] string",
                    periodStr), ex);
        }

        try {
            endPeriod = LocalDate.parse(components[1],
                    periodStringDateTimeFormatter());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(String.format(
                    "Can't parse statement period end date from [%s] string",
                    periodStr), ex);
        }

        if (!endPeriod.isAfter(beginPeriod)) {
            throw new IllegalArgumentException(String.format(
                    "End period date [%s] should follow begin period date [%s]",
                    endPeriod, beginPeriod));
        }

        if (ChronoUnit.YEARS.between(Year.from(beginPeriod),
                Year.from(endPeriod)) > 1) {
            throw new IllegalArgumentException(String.format(
                    "End [%s] and begin [%s] period dates should be in the same year or in the adjacent years",
                    endPeriod, beginPeriod));
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

    private LocalDate beginPeriod;
    private LocalDate endPeriod;

    private final static DateTimeFormatter MONTH_DAY_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "MMM dd", Locale.US);
}
