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
        transactionLogRepository.save(transaction);
        transactionFileLogger.appendTransactionExecuted(transaction);
    }
}
