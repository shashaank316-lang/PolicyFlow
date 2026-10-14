
package com.policyflow.claimsservice.replay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.springframework.kafka.support.SendResult;

import java.util.concurrent.ExecutionException;

class DltReplayServiceTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private DltReplayService dltReplayService;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = new ObjectMapper();
        dltReplayService = new DltReplayService(kafkaTemplate, objectMapper);
    }

    @Test
    void shouldRejectBlankPayload() {
        assertThrows(
                IllegalArgumentException.class,
                () -> dltReplayService.replay(" ")
        );
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectInvalidJson() {
        assertThrows(
                Exception.class,
                () -> dltReplayService.replay("not-valid-json")
        );
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectEventMissingRequiredFields() {
        String payload = """
                {
                  "claimNumber": "CLM-TEST-001"
                }
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> dltReplayService.replay(payload)
        );
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldRejectUnsupportedEventType() {
        String payload = """
                {
                  "eventId": "TEST-EVENT-001",
                  "eventType": "UNSUPPORTED_EVENT"
                }
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> dltReplayService.replay(payload)
        );
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldPublishValidClaimCreatedEvent() throws Exception {
        String payload = """
                {
                  "eventId": "TEST-EVENT-002",
                  "eventType": "CLAIM_CREATED",
                  "claimNumber": "CLM-TEST-002",
                  "policyNumber": "POL-40003",
                  "customerId": "CUS-10001",
                  "claimType": "ACCIDENT",
                  "claimAmount": 100.00,
                  "eventTimestamp": "2026-10-12T17:00:00Z"
                }
                """;

        when(kafkaTemplate.send(
                eq("claim-events"),
                eq("CLM-TEST-002"),
                eq(payload)
        )).thenReturn(CompletableFuture.completedFuture(null));

        assertDoesNotThrow(() -> dltReplayService.replay(payload));

        verify(kafkaTemplate).send(
                eq("claim-events"),
                eq("CLM-TEST-002"),
                anyString()
        );
    }

    @Test
    void shouldPropagateKafkaPublicationFailure() throws Exception {
        String payload = """
            {
              "eventId": "TEST-EVENT-FAILURE-001",
              "eventType": "CLAIM_CREATED",
              "claimNumber": "CLM-TEST-FAILURE-001",
              "policyNumber": "POL-40003",
              "customerId": "CUS-10001",
              "claimType": "ACCIDENT",
              "claimAmount": 100.00,
              "eventTimestamp": "2026-10-12T17:00:00Z"
            }
            """;

        CompletableFuture<SendResult<String, String>> failedFuture =
                new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException("Simulated Kafka failure")
        );

        when(kafkaTemplate.send(
                eq("claim-events"),
                eq("CLM-TEST-FAILURE-001"),
                eq(payload)
        )).thenReturn(failedFuture);

        assertThrows(
                ExecutionException.class,
                () -> dltReplayService.replay(payload)
        );

        verify(kafkaTemplate).send(
                eq("claim-events"),
                eq("CLM-TEST-FAILURE-001"),
                anyString()
        );
    }
}
