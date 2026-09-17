package com.royal.reserve.bank.notification.api.integration;

import com.royal.reserve.bank.notification.api.NotificationApiApplication;
import com.royal.reserve.bank.notification.api.event.TransactionEvent;
import com.royal.reserve.bank.notification.api.listener.NotificationListener;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for the {@link NotificationApiApplication}: boots the context against an
 * embedded Kafka broker and verifies that a {@link TransactionEvent} published to the topic is
 * deserialized and delivered to the {@link NotificationListener}.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.kafka.consumer.group-id=notificationId",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer="
                + "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
        "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class="
                + "org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.royal.reserve.bank.*",
        "spring.kafka.consumer.properties.spring.json.value.default.type="
                + "com.royal.reserve.bank.notification.api.event.TransactionEvent",
        "spring.kafka.consumer.properties.spring.json.type.mapping="
                + "event:com.royal.reserve.bank.notification.api.event.TransactionEvent",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.type.mapping="
                + "event:com.royal.reserve.bank.notification.api.event.TransactionEvent"
})
@EmbeddedKafka(partitions = 1, topics = NotificationListener.TOPIC,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class NotificationApiApplicationIT {

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockitoSpyBean
    private NotificationListener notificationListener;

    private KafkaTemplate<String, String> rawKafkaTemplate;

    @BeforeEach
    void setUpRawTemplate() {
        Map<String, Object> props = KafkaTestUtils.producerProps(embeddedKafka);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        rawKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Test
    void contextLoads() {
        assertThat(notificationListener).isNotNull();
    }

    @Test
    void shouldConsumeTransactionEventFromTopic() {
        String transactionId = "13645940";

        kafkaTemplate.send(NotificationListener.TOPIC, new TransactionEvent(transactionId));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(notificationListener).handleNotification(
                        argThat(event -> transactionId.equals(event.getTransactionId()))));
    }

    @Test
    void shouldSkipPoisonPillAndKeepConsuming() {
        String transactionId = "after-poison-pill";

        rawKafkaTemplate.send(NotificationListener.TOPIC, "this is not json");
        rawKafkaTemplate.send(NotificationListener.TOPIC, "{\"transactionId\":\"" + transactionId + "\"}");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                verify(notificationListener).handleNotification(
                        argThat(event -> transactionId.equals(event.getTransactionId()))));
    }
}
