package com.example.posdemo.transaction;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MongoTransactionLogRepository implements TransactionLogRepository {

    private static final String EVENT_TYPE_TRANSACTION_EXECUTED = "TRANSACTION_EXECUTED";

    private final MongoTemplate mongoTemplate;
    private final String collection;

    public MongoTransactionLogRepository(
            MongoTemplate mongoTemplate,
            @Value("${pos.mongodb.collection:transaction_logs}") String collection
    ) {
        this.mongoTemplate = mongoTemplate;
        this.collection = collection;
    }

    @Override
    public void save(Transaction transaction) {
        mongoTemplate.insert(toDocument(transaction), collection);
    }

    private TransactionLogDocument toDocument(Transaction transaction) {
        List<TransactionLogDocument.TransactionLogItemDocument> items = transaction.items()
                .stream()
                .map(item -> new TransactionLogDocument.TransactionLogItemDocument(
                        item.productName(),
                        item.quantity(),
                        item.unitPrice(),
                        item.lineTotal()
                ))
                .toList();

        return new TransactionLogDocument(
                transaction.transactionId(),
                EVENT_TYPE_TRANSACTION_EXECUTED,
                transaction.timestamp(),
                transaction.cashierId(),
                items,
                transaction.totalAmount(),
                transaction.paymentMethod()
        );
    }
}
