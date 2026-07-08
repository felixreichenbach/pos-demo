package com.example.posdemo.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Transaction(
        String transactionId,
        Instant timestamp,
        String cashierId,
        List<TransactionItem> items,
        BigDecimal totalAmount,
        String paymentMethod
) {
    public Transaction {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(cashierId, "cashierId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(paymentMethod, "paymentMethod must not be null");
        items = List.copyOf(items);
    }
}
