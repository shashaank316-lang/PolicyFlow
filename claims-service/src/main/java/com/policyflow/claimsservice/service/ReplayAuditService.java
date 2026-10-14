package com.policyflow.claimsservice.service;

import com.policyflow.claimsservice.audit.ReplayAudit;
import com.policyflow.claimsservice.repository.ReplayAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplayAuditService {

    private final ReplayAuditRepository replayAuditRepository;

    public ReplayAuditService(
            ReplayAuditRepository replayAuditRepository) {
        this.replayAuditRepository = replayAuditRepository;
    }

    @Transactional
    public void record(
            String eventId,
            String eventType,
            String outcome,
            String reason) {

        ReplayAudit audit = new ReplayAudit(
                eventId,
                eventType,
                outcome,
                reason
        );

        replayAuditRepository.save(audit);
    }
}
