package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.CsvReader;
import static com.budgetmaster.budgetmaster.amazon.AmazonRefundsCsv.DATE_FORMATTER;
import static com.budgetmaster.budgetmaster.amazon.AmazonRefundsCsv.parseAmountUSD;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Currency;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


final class AmazonOrdersCsv {
    Stream<AmazonOrder> read(Path file) throws IOException {
        return new CsvReader<AmazonOrder>()
        .setParser(initParser())
        .setConv((csvRecord, reportError) -> {
            AmazonOrder result = new AmazonOrder();

            result.date = LocalDate.parse(csvRecord.get(Headers.OrderDate),
                    DATE_FORMATTER);

            result.orderId = csvRecord.get(Headers.OrderID);

            result.title = csvRecord.get(Headers.Title);

            result.currency = Currency.getInstance(csvRecord.get(
                    Headers.Currency));

            result.amount = parseAmountUSD(csvRecord.get(Headers.ItemTotal));

            return result;
        }).readCsv(file);
    }

    private CSVFormat initParser() {
        return CSVFormat.RFC4180.withFirstRecordAsHeader().withHeader(Headers.class);
    }

    enum Headers {
        OrderDate,
        OrderID,
        Title,
        Category,
        ASIN_ISBN,
        UNSPSC_Code,
        Website,
        ReleaseDate,
        Condition,
        Seller,
        SellerCredentials,
        ListPricePerUnit,
        PurchasePricePerUnit,
        Quantity,
        PaymentInstrument,
        Type,
        PurchaseOrderNumber,
        POLineNumber,
        Ordering,
        Customer,
        Email,
        ShipmentDate,
        ShippingAddressName,
        ShippingAddressStreet1,
        ShippingAddressStreet2,
        ShippingAddressCity,
        ShippingAddressState,
        ShippingAddressZip,
        OrderStatus,
        CarrierName_and_TrackingNumber,
        ItemSubtotal,
        ItemSubtotalTax,
        ItemTotal,
        TaxExemptionApplied,
        TaxExemptionType,
        ExemptionOpt_Out,
        BuyerName,
        Currency,
        GroupName
    };
}
