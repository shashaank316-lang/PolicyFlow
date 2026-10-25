package com.policyflow.claimsservice.integration;

import com.policyflow.claimsservice.outbox.OutboxEvent;
import com.policyflow.claimsservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "app.security.username=test-user",
    "app.security.password=test-password"
})
class OutboxRecoveryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("claims_recovery_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final KafkaContainer kafka =
        new KafkaContainer("apache/kafka:4.1.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers);
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoSpyBean(name = "outboxKafkaTemplate")
    private KafkaTemplate<String, String> outboxKafkaTemplate;

    @Test
    void shouldRetryPendingOutboxEventAfterTemporaryPublishFailure()
            throws Exception {

        String eventId = UUID.randomUUID().toString();
        String claimNumber = "RECOVERY-" + eventId;

        OutboxEvent event = new OutboxEvent(
            eventId,
            "CLAIM_CREATED",
            claimNumber,
            """
            {
              "eventId": "%s",
              "eventType": "CLAIM_CREATED",
              "claimNumber": "%s"
            }
            """.formatted(eventId, claimNumber)
        );

        // Simulate one transient failure, then use the real Kafka producer.
        doThrow(new RuntimeException("Simulated temporary Kafka failure"))
            .doCallRealMethod()
            .when(outboxKafkaTemplate)
            .send(eq("claim-events"), eq(claimNumber), anyString());

        outboxEventRepository.saveAndFlush(event);

        OutboxEvent pendingEvent = outboxEventRepository.findAll()
            .stream()
            .filter(item -> eventId.equals(item.getEventId()))
            .findFirst()
            .orElseThrow();

        assertEquals("PENDING", pendingEvent.getStatus());
        assertNull(pendingEvent.getPublishedAt());

        // The scheduled publisher retries every five seconds.
        // The first attempt fails; a later attempt should reach Kafka.
        awaitCondition(
            () -> outboxEventRepository.findAll()
                .stream()
                .anyMatch(item ->
                    eventId.equals(item.getEventId())
                        && "PUBLISHED".equals(item.getStatus())
                        && item.getPublishedAt() != null),
            Duration.ofSeconds(45),
            "The outbox event was not published after the temporary failure"
        );

        OutboxEvent publishedEvent = outboxEventRepository.findAll()
            .stream()
            .filter(item -> eventId.equals(item.getEventId()))
            .findFirst()
            .orElseThrow();

        assertEquals("PUBLISHED", publishedEvent.getStatus());
        assertNotNull(publishedEvent.getPublishedAt());
    }

    private static void awaitCondition(
            BooleanSupplier condition,
            Duration timeout,
            String failureMessage) throws InterruptedException {

        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }

            TimeUnit.MILLISECONDS.sleep(250);
        }

        assertTrue(condition.getAsBoolean(), failureMessage);
    }
}
