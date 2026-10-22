
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

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
}
