package com.example.posdemo.transaction.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateTransactionItemRequest(
        @NotBlank String productName,
        @Positive int quantity,
        @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice
) {
}
