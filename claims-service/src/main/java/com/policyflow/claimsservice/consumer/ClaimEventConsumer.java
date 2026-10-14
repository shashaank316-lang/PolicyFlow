package com.policyflow.claimsservice.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ClaimEventConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(ClaimEventConsumer.class);

    private final ClaimEventProcessingService processingService;
    private final ObjectMapper objectMapper;

    public ClaimEventConsumer(
            ClaimEventProcessingService processingService,
            ObjectMapper objectMapper) {
        this.processingService = processingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "claim-events",
            groupId = "policyflow-claims-consumer"
    )
    public void consume(String message) throws Exception {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Kafka event must not be empty"
            );
        }

        JsonNode event = objectMapper.readTree(message);

        if (event == null || !event.isObject()) {
            throw new IllegalArgumentException(
                    "Kafka event must be a JSON object"
            );
        }

        String eventId = event.path("eventId").asString("");
        String eventType = event.path("eventType").asString("");

        if (eventId.isBlank() || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Kafka event must contain eventId and eventType"
            );
        }

        boolean processed = processingService.process(eventId, eventType);

        if (processed) {
            logger.info(
                    "Claim event processed successfully: eventId={}, eventType={}",
                    eventId, eventType
            );
        } else {
            logger.info(
                    "Claim event was already processed: eventId={}",
                    eventId
            );
        }
    }
}
