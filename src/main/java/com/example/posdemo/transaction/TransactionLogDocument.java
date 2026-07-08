package com.example.posdemo.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionLogDocument(
        String transactionId,
        String eventType,
        Instant timestamp,
        String cashierId,
        List<TransactionLogItemDocument> items,
        BigDecimal totalAmount,
        String paymentMethod
) {
    public record TransactionLogItemDocument(
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
