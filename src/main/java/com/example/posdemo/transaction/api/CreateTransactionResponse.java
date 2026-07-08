package com.example.posdemo.transaction.api;

import java.time.Instant;

public record CreateTransactionResponse(
        String transactionId,
        Instant timestamp,
        String status
) {
}
