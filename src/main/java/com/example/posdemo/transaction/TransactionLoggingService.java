package com.example.posdemo.transaction;

import org.springframework.stereotype.Service;

@Service
public class TransactionLoggingService {

    private final TransactionLogRepository transactionLogRepository;
    private final TransactionFileLogger transactionFileLogger;

    public TransactionLoggingService(
            TransactionLogRepository transactionLogRepository,
            TransactionFileLogger transactionFileLogger
    ) {
        this.transactionLogRepository = transactionLogRepository;
        this.transactionFileLogger = transactionFileLogger;
    }

    public void logExecutedTransaction(Transaction transaction) {
        try {
            transactionLogRepository.save(transaction);
            transactionFileLogger.appendTransactionExecuted(transaction);
        } catch (RuntimeException exception) {
            String errorDetail = exception.getMessage() == null ? "Unknown error" : exception.getMessage();

            try {
                transactionFileLogger.appendTransactionFailed(transaction, errorDetail);
            } catch (RuntimeException fileLoggingException) {
                exception.addSuppressed(fileLoggingException);
            }

            throw exception;
        }
    }
}
