package com.policyflow.claimsservice.consumer;

import com.policyflow.claimsservice.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessedEventWriter {

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventWriter(
            ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void record(String eventId, String eventType) {
        processedEventRepository.saveAndFlush(
                new ProcessedEvent(eventId, eventType)
        );
    }
}
