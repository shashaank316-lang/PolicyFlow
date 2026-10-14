
package com.policyflow.claimsservice.replay;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;

@Service
public class DltReplayService {

    private static final String SOURCE_TOPIC = "claim-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DltReplayService(
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void replay(String payload) throws Exception {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Replay payload must not be empty");
        }

        JsonNode event = objectMapper.readTree(payload);

        if (event == null || !event.isObject()) {
            throw new IllegalArgumentException(
                    "Replay payload must be a JSON object");
        }

        String eventId = event.path("eventId").asString("");
        String eventType = event.path("eventType").asString("");

        if (eventId.isBlank() || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Replay event must contain eventId and eventType");
        }

        if (!"CLAIM_CREATED".equals(eventType)) {
            throw new IllegalArgumentException(
                    "Unsupported replay eventType: " + eventType);
        }

        kafkaTemplate.send(
                SOURCE_TOPIC,
                event.path("claimNumber").asString(eventId),
                payload
        ).get(10, TimeUnit.SECONDS);
    }
}
