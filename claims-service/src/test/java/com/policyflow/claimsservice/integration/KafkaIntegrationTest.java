
package com.policyflow.claimsservice.integration;

import com.policyflow.claimsservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.policyflow.claimsservice.consumer.ClaimEventProcessingService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {"app.security.username=test-user", "app.security.password=test-password"})
@Testcontainers
class KafkaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("claims_test")
                    .withUsername("test")
                    .withPassword("test");
    @Container
    static final KafkaContainer kafka =
            new KafkaContainer("apache/kafka:4.1.0");
    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    @MockitoSpyBean
    private ClaimEventProcessingService processingService;

    @Autowired
    private KafkaTemplate<String, String> outboxKafkaTemplate;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void shouldPublishAndProcessMessageThroughRealKafka() throws Exception {
        String topic = "claim-events";
        String eventId = UUID.randomUUID().toString();
        String claimNumber = "KAFKA-TEST-" + eventId;

        String payload = """
                {
                  "eventId": "%s",
                  "eventType": "CLAIM_CREATED",
                  "claimNumber": "%s"
                }
                """.formatted(eventId, claimNumber);

        // Publish to the real Kafka broker and wait for its acknowledgment.
        SendResult<String, String> result =
                outboxKafkaTemplate
                        .send(topic, claimNumber, payload)
                        .get(10, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(topic, result.getRecordMetadata().topic());

        // Wait for the application's Kafka listener to record the event.
        long deadline =
                System.nanoTime() + Duration.ofSeconds(15).toNanos();

        boolean processed = false;

        while (System.nanoTime() < deadline) {
            if (processedEventRepository.existsByEventId(eventId)) {
                processed = true;
                break;
            }

            Thread.sleep(250);
        }

        assertTrue(
                processed,
                "The Claims Service consumer did not record eventId: " + eventId
        );
    }

    @Test
    void shouldIgnoreDuplicateEventDelivery() throws Exception {
        String topic = "claim-events";
        String eventId = UUID.randomUUID().toString();
        String claimNumber = "KAFKA-DUPLICATE-" + eventId;

        String payload = """
            {
              "eventId": "%s",
              "eventType": "CLAIM_CREATED",
              "claimNumber": "%s"
            }
            """.formatted(eventId, claimNumber);

        // Publish the first delivery.
        outboxKafkaTemplate
                .send(topic, claimNumber, payload)
                .get(10, TimeUnit.SECONDS);

        // Wait for the consumer to persist the first delivery.
        long deadline =
                System.nanoTime() + Duration.ofSeconds(15).toNanos();

        while (System.nanoTime() < deadline
                && !processedEventRepository.existsByEventId(eventId)) {
            Thread.sleep(250);
        }

        assertTrue(
                processedEventRepository.existsByEventId(eventId),
                "The first delivery was not processed"
        );

        // Publish the exact same event again.
        outboxKafkaTemplate
                .send(topic, claimNumber, payload)
                .get(10, TimeUnit.SECONDS);

        // Allow the consumer time to handle the duplicate.
        // Verify that the consumer handled the second delivery.
        verify(processingService, timeout(10000).atLeast(2))
                .process(eq(eventId), eq("CLAIM_CREATED"));
        // The same event ID must still have only one database record.
        long matchingRecords = processedEventRepository.findAll()
                .stream()
                .filter(event -> eventId.equals(event.getEventId()))
                .count();

        assertEquals(
                1,
                matchingRecords,
                "Duplicate delivery must not create another processed-event record"
        );
    }

}
