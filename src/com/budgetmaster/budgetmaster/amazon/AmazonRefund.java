package com.budgetmaster.budgetmaster.amazon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

final class AmazonRefund {
    String orderId;
    LocalDate date;
    String title;
    BigDecimal amount;
    Currency currency;
}
