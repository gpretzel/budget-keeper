package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.CsvReader;
import com.budgetmaster.budgetmaster.MonetaryAmount;
import com.budgetmaster.budgetmaster.Util;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;


final class AmazonRefundsCsv {
    Stream<AmazonRefund> read(Path file) throws IOException {
        return new CsvReader<AmazonRefund>()
        .setParser(initParser())
        .setConv((csvRecord, reportError) -> {
            AmazonRefund result = new AmazonRefund();

            result.date = LocalDate.parse(csvRecord.get(Headers.OrderDate),
                    DATE_FORMATTER);

            result.orderId = csvRecord.get(Headers.OrderID);

            result.title = csvRecord.get(Headers.Title);

            result.currency = Util.USD;

            BigDecimal refundAmount = parseAmountUSD(csvRecord.get(
                    Headers.RefundAmount));
            BigDecimal refundTaxAmount = parseAmountUSD(csvRecord.get(
                    Headers.RefundTaxAmount));

            result.amount = refundAmount.add(refundTaxAmount);

            return result;
        }).readCsv(file);
    }

    private CSVFormat initParser() {
        return CSVFormat.RFC4180.withFirstRecordAsHeader().withHeader(Headers.class);
    }

    enum Headers {
        OrderID,
        OrderDate,
        Title,
        Category,
        ASIN_ISBN,
        Website,
        Purchase,
        OrderNumber,
        RefundDate,
        RefundCondition,
        RefundAmount,
        RefundTaxAmount,
        TaxExemptionApplied,
        RefundReason,
        Quantity,
        Seller,
        SellerCredentials,
        BuyerName,
        GroupName
    };

    final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "dd/MM/yy");

    static BigDecimal parseAmountUSD(String value) {
        return MonetaryAmount.of(value.substring(1));
    }
}
