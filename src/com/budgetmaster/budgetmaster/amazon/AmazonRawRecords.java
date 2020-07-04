package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.CsvReader;
import com.budgetmaster.budgetmaster.Functional;
import com.budgetmaster.budgetmaster.Functional.ThrowingConsumer;
import com.budgetmaster.budgetmaster.Util;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

final class AmazonRawRecords {
    static AmazonRawRecords load(Path csvFile,
            Class<? extends Enum<?>> headerEnum) throws IOException {
        List<String> headers = new ArrayList<>();
        var reader = new CsvReader<AmazonRawRecord>()
        .setFormat(CSVFormat.RFC4180
                .withFirstRecordAsHeader()
                .withHeader(headerEnum))
        .setHeaderNamesConsumer(headers::addAll)
        .setConv((CSVRecord csvRecord, BiConsumer<String, Exception> reportError) -> {
            final String[] data = CsvReader.read(csvRecord);
            final long lineNumber = csvRecord.getParser().getCurrentLineNumber();
            return new AmazonRawRecord() {
                @Override
                public String[] getData() {
                    return data;
                }

                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("#").append(lineNumber);
                    sb.append("|");
                    sb.append(Util.pathEllipsis(csvFile.toAbsolutePath(), 40));
                    return sb.toString();
                }
            };
        });

        List<AmazonRawRecord> data = reader.readCsv(csvFile).collect(
                Collectors.toList());

        return new AmazonRawRecords(headers, data);
    }

    void save(Path csvFile, Predicate<AmazonRawRecord> filter) throws IOException {
        if ("--".equals(csvFile.toString())) {
            StringBuilder sb = new StringBuilder();
            save(sb, filter);
            System.out.append(sb.toString());
        } else {
            try (Writer writer = Files.newBufferedWriter(csvFile)) {
                save(writer, filter);
            }
        }
    }

    Stream<AmazonRawRecord> getData() {
        return data.stream();
    }

    private void save(Appendable appendable, Predicate<AmazonRawRecord> filter) throws
            IOException {
        try (CSVPrinter csvPrinter = new CSVPrinter(appendable,
                CSVFormat.RFC4180.withHeader(headers.toArray(String[]::new)))) {
            data.stream()
                    .filter(filter)
                    .map(AmazonRawRecord::getData)
                    .forEachOrdered(ThrowingConsumer.toConsumer(
                            csvPrinter::printRecord));
        } catch (Functional.ExceptionBox ex) {
            var cause = ex.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw ex;
        }
    }

    private AmazonRawRecords(List<String> headers, List<AmazonRawRecord> data) {
        this.headers = headers;
        this.data = data;
    }

    private final List<String> headers;
    private final List<AmazonRawRecord> data;
}
