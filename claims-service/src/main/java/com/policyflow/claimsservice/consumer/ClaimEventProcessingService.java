package com.policyflow.claimsservice.consumer;

import com.policyflow.claimsservice.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimEventProcessingService {

    private static final Logger logger =
            LoggerFactory.getLogger(ClaimEventProcessingService.class);

    private final ProcessedEventRepository processedEventRepository;

    public ClaimEventProcessingService(
            ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean process(String eventId, String eventType) {

        if (processedEventRepository.existsByEventId(eventId)) {
            logger.info(
                    "Duplicate event ignored. eventId={}",
                    eventId
            );
            return false;
        }

        try {
            processedEventRepository.saveAndFlush(
                    new ProcessedEvent(eventId, eventType)
            );

            logger.info(
                    "Event recorded. eventId={}, eventType={}",
                    eventId,
                    eventType
            );
            return true;

        } catch (DataIntegrityViolationException exception) {
            logger.info(
                    "Duplicate event rejected by database constraint. eventId={}",
                    eventId
            );
            return false;
        }
    }
}
