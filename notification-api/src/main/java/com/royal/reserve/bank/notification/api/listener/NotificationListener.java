package com.royal.reserve.bank.notification.api.listener;

import com.royal.reserve.bank.notification.api.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for transaction events published by the Transaction API.
 */
@Component
@Slf4j
public class NotificationListener {

    public static final String TOPIC = "notificationTopic";

    /**
     * Handles a {@link TransactionEvent} received from the {@value #TOPIC} topic.
     *
     * @param transactionEvent the event received from Kafka
     */
    @KafkaListener(topics = TOPIC)
    public void handleNotification(TransactionEvent transactionEvent) {
        log.info("Received notification for transaction: {}", transactionEvent.getTransactionId());
    }
}
