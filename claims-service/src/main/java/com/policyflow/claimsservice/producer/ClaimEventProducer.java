
package com.policyflow.claimsservice.producer;

import com.policyflow.claimsservice.entity.Claim;
import com.policyflow.claimsservice.event.ClaimCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ClaimEventProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(ClaimEventProducer.class);

    private static final String TOPIC = "claim-events";

    private final KafkaTemplate<String, ClaimCreatedEvent> kafkaTemplate;

    public ClaimEventProducer(
            KafkaTemplate<String, ClaimCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<Void> publishClaimCreated(Claim claim) {

        ClaimCreatedEvent event = new ClaimCreatedEvent(
                UUID.randomUUID().toString(),
                "CLAIM_CREATED",
                claim.getClaimNumber(),
                claim.getPolicyNumber(),
                claim.getCustomerId(),
                claim.getClaimType(),
                claim.getClaimAmount(),
                Instant.now()
        );

        return kafkaTemplate
                .send(TOPIC, claim.getClaimNumber(), event)
                .thenAccept(result -> logger.info(
                        "Published event {} for claim {} to topic {} partition {} offset {}",
                        event.eventId(),
                        event.claimNumber(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                ))
                .exceptionally(error -> {
                    logger.error(
                            "Failed to publish event for claim {}",
                            claim.getClaimNumber(),
                            error
                    );
                    throw new java.util.concurrent.CompletionException(error);
                });
    }
}
