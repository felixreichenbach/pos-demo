package com.example.posdemo.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionLoggingServiceTest {

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private TransactionFileLogger transactionFileLogger;

    @InjectMocks
    private TransactionLoggingService transactionLoggingService;

    @Test
    void logsTransactionToDatabaseAndDedicatedFile() {
        Transaction transaction = sampleTransaction();

        transactionLoggingService.logExecutedTransaction(transaction);

        InOrder callOrder = inOrder(transactionLogRepository, transactionFileLogger);
        callOrder.verify(transactionLogRepository).save(transaction);
        callOrder.verify(transactionFileLogger).appendTransactionExecuted(transaction);
    }

    @Test
    void throwsAndSkipsFileLoggingWhenDatabaseWriteFails() {
        doThrow(new RuntimeException("database unavailable"))
                .when(transactionLogRepository)
                .save(any(Transaction.class));

        Transaction transaction = sampleTransaction();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> transactionLoggingService.logExecutedTransaction(transaction)
        );

        assertEquals("database unavailable", exception.getMessage());
        verify(transactionFileLogger, never()).appendTransactionExecuted(any(Transaction.class));
    }

    private Transaction sampleTransaction() {
        TransactionItem item = new TransactionItem(
                "Coffee",
                2,
                new BigDecimal("3.50"),
                new BigDecimal("7.00")
        );

        return new Transaction(
                "TXN-1001",
                Instant.parse("2026-07-08T10:15:30Z"),
                "cashier-01",
                List.of(item),
                new BigDecimal("7.00"),
                "CARD"
        );
    }
}
