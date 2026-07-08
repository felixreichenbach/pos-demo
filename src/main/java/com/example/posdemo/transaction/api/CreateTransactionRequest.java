package com.example.posdemo.transaction.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateTransactionRequest(
        @NotBlank String cashierId,
        @NotEmpty List<@Valid CreateTransactionItemRequest> items,
        @NotBlank String paymentMethod
) {
}
