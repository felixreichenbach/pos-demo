package com.example.posdemo.transaction.api;

import com.example.posdemo.transaction.Transaction;
import com.example.posdemo.transaction.TransactionItem;
import com.example.posdemo.transaction.TransactionLoggingService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionLoggingService transactionLoggingService;

    public TransactionController(TransactionLoggingService transactionLoggingService) {
        this.transactionLoggingService = transactionLoggingService;
    }

    @PostMapping
    public ResponseEntity<CreateTransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        Instant timestamp = Instant.now();
        String transactionId = UUID.randomUUID().toString();

        List<TransactionItem> items = request.items()
                .stream()
                .map(this::toTransactionItem)
                .toList();

        BigDecimal calculatedTotal = items.stream()
                .map(TransactionItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Transaction transaction = new Transaction(
                transactionId,
                timestamp,
                request.cashierId(),
                items,
            calculatedTotal,
                request.paymentMethod()
        );

        transactionLoggingService.logExecutedTransaction(transaction);

        CreateTransactionResponse response = new CreateTransactionResponse(
                transactionId,
                timestamp,
                "LOGGED"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private TransactionItem toTransactionItem(CreateTransactionItemRequest item) {
        BigDecimal lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
        return new TransactionItem(item.productName(), item.quantity(), item.unitPrice(), lineTotal);
    }
}
