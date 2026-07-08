package com.example.posdemo.transaction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalTransactionFileLogger implements TransactionFileLogger {

    private final Path transactionLogPath;

    public LocalTransactionFileLogger(
            @Value("${pos.logging.transaction-log-file:./logs/transactions.log}") String transactionLogPath
    ) {
        this.transactionLogPath = Path.of(transactionLogPath);
    }

    @Override
    public void appendTransactionExecuted(Transaction transaction) {
        String line = String.format(
                "timestamp=%s status=SUCCESS eventType=TRANSACTION_EXECUTED transactionId=%s cashierId=%s totalAmount=%s paymentMethod=%s%n",
                transaction.timestamp(),
                transaction.transactionId(),
                transaction.cashierId(),
                transaction.totalAmount(),
                transaction.paymentMethod()
        );

        appendLine(line);
    }

    @Override
    public void appendTransactionFailed(Transaction transaction, String errorDetail) {
        String line = String.format(
                "timestamp=%s status=FAILED eventType=TRANSACTION_EXECUTED transactionId=%s cashierId=%s totalAmount=%s paymentMethod=%s error=%s%n",
                transaction.timestamp(),
                transaction.transactionId(),
                transaction.cashierId(),
                transaction.totalAmount(),
                transaction.paymentMethod(),
                errorDetail
        );

        appendLine(line);
    }

    private void appendLine(String line) {

        try {
            Path parent = transactionLogPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    transactionLogPath,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to append transaction log file entry", exception);
        }
    }
}
