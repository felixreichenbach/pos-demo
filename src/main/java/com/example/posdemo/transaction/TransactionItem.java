package com.example.posdemo.transaction;

import java.math.BigDecimal;
import java.util.Objects;

public record TransactionItem(
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public TransactionItem {
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(lineTotal, "lineTotal must not be null");
    }
}
