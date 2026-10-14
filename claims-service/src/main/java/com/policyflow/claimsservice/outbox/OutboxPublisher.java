
package com.policyflow.claimsservice.outbox;

import com.policyflow.claimsservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private static final Logger logger =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private static final String TOPIC = "claim-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        var events =
                outboxEventRepository.findTop100ByStatusOrderByIdAsc("PENDING");

        for (OutboxEvent event : events) {
            try {
                // Wait for Kafka's acknowledgment before marking published.
                kafkaTemplate.send(
                        TOPIC,
                        event.getAggregateId(),
                        event.getPayload()
                ).get();

                markAsPublished(event);

                logger.info(
                        "Published outbox event {} for claim {}",
                        event.getEventId(),
                        event.getAggregateId()
                );

            } catch (Exception exception) {
                logger.error(
                        "Failed to publish outbox event {}. It will be retried.",
                        event.getEventId(),
                        exception
                );
            }
        }
    }

    @Transactional
    public void markAsPublished(OutboxEvent event) {
        event.markPublished();
        outboxEventRepository.save(event);
    }
}
