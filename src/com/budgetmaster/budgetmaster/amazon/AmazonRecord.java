package com.budgetmaster.budgetmaster.amazon;

import com.budgetmaster.budgetmaster.MonetaryAmount;
import com.budgetmaster.budgetmaster.Util;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

interface AmazonRecord {

    String getOrderId();

    LocalDate getDate();

    String getTitle();

    BigDecimal getAmount();

    final static class Impl implements AmazonRecord {

        @Override
        public String getOrderId() {
            return orderId;
        }

        @Override
        public LocalDate getDate() {
            return date;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public BigDecimal getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return String.join("|", getOrderId(), String.format("%s", getDate()),
                    getAmount().toPlainString(), getTitle(), toStringValue);
        }

        private Impl(String orderId, LocalDate date, String title,
                BigDecimal amount, String toStringValue) {
            this.orderId = orderId;
            this.date = date;
            this.title = title;
            this.amount = amount;
            this.toStringValue = toStringValue;
        }

        private final String orderId;

        private final LocalDate date;

        private final String title;

        private final BigDecimal amount;

        private final String toStringValue;
    }

    static AmazonRecord order(AmazonRawRecord value) {
        final String[] data = value.getData();

        String orderId = data[CsvOrderHeaders.OrderID.ordinal()];

        LocalDate date = LocalDate.parse(
                data[CsvOrderHeaders.ShipmentDate.ordinal()], DATE_FORMATTER);

        String title = data[CsvOrderHeaders.Title.ordinal()];

        int quantity = Integer.parseInt(data[CsvOrderHeaders.Quantity.ordinal()]);
        if (quantity > 1) {
            title = String.format("%s (%d)", title, quantity);
        }

        BigDecimal amount = MonetaryAmount.of(
                data[CsvOrderHeaders.ItemTotal.ordinal()]).getAmount();

        return new Impl(orderId, date, title, amount, value.toString());
    }

    static AmazonRecord order(List<? extends AmazonRecord> orders) {
        String orderId = orders.get(0).getOrderId();

        LocalDate date = orders.get(0).getDate();

        String title = orders.stream().map(AmazonRecord::getTitle).collect(
                Collectors.joining("|"));

        BigDecimal amount = orders.stream().map(AmazonRecord::getAmount).reduce(
                BigDecimal.ZERO, BigDecimal::add);

        return new Impl(orderId, date, title, amount, String.format("group(%d)",
                orders.size()));
    }

    static AmazonRecord refund(AmazonRawRecord value) {
        final String[] data = value.getData();

        String orderId = data[CsvRefundHeaders.OrderID.ordinal()];

        LocalDate date = LocalDate.parse(
                data[CsvRefundHeaders.RefundDate.ordinal()], DATE_FORMATTER);

        String title = data[CsvRefundHeaders.Title.ordinal()];

        BigDecimal refundAmount = MonetaryAmount.of(
                data[CsvRefundHeaders.RefundAmount.ordinal()]).getAmount();
        BigDecimal refundTaxAmount = MonetaryAmount.of(
                data[CsvRefundHeaders.RefundTaxAmount.ordinal()]).getAmount();

        BigDecimal amount = refundAmount.add(refundTaxAmount).negate();

        return new Impl(orderId, date, title, amount, value.toString());
    }

    final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "MM/dd/yy");

    enum CsvOrderHeaders {
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
        PaymentInstrumentType,
        PurchaseOrderNumber,
        POLineNumber,
        OrderingCustomerEmail,
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

    enum CsvRefundHeaders {
        OrderID,
        OrderDate,
        Title,
        Category,
        ASIN_ISBN,
        Website,
        PurchaseOrderNumber,
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
}
