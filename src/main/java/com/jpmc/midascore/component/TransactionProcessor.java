package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    private final DatabaseConduit databaseConduit;

    public TransactionProcessor(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }

    @Transactional
    public void process(Transaction transaction) {
        UserRecord sender = databaseConduit.findById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.findById(transaction.getRecipientId());

        if (sender != null && recipient != null && sender.getBalance() >= transaction.getAmount()) {
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount());

            databaseConduit.save(sender);
            databaseConduit.save(recipient);

            TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount());
            databaseConduit.saveTransaction(record);
            logger.info("Processed transaction: {}", record);
        } else {
            logger.warn("Discarded invalid transaction: {}", transaction);
        }
    }
}
