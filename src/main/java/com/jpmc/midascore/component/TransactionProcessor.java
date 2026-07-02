package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    private final DatabaseConduit databaseConduit;
    private final RestTemplate restTemplate;
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    public TransactionProcessor(DatabaseConduit databaseConduit, RestTemplate restTemplate) {
        this.databaseConduit = databaseConduit;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void process(Transaction transaction) {
        UserRecord sender = databaseConduit.findById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.findById(transaction.getRecipientId());

        if (sender != null && recipient != null && sender.getBalance() >= transaction.getAmount()) {
            float incentiveAmount = 0f;
            try {
                Incentive incentive = restTemplate.postForObject(INCENTIVE_API_URL, transaction, Incentive.class);
                if (incentive != null) {
                    incentiveAmount = incentive.getAmount();
                }
            } catch (Exception e) {
                logger.error("Failed to get incentive amount from API, defaulting to 0", e);
            }

            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

            databaseConduit.save(sender);
            databaseConduit.save(recipient);

            TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
            databaseConduit.saveTransaction(record);
            logger.info("Processed transaction: {}", record);
        } else {
            logger.warn("Discarded invalid transaction: {}", transaction);
        }
    }
}
