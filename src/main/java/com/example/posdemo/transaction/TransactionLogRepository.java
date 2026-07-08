package com.example.posdemo.transaction;

public interface TransactionLogRepository {

    void save(Transaction transaction);
}
