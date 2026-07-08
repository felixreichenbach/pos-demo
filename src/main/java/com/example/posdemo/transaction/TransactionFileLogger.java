package com.example.posdemo.transaction;

public interface TransactionFileLogger {

    void appendTransactionExecuted(Transaction transaction);

    void appendTransactionFailed(Transaction transaction, String errorDetail);
}
