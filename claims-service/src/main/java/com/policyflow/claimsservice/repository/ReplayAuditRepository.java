package com.policyflow.claimsservice.repository;

import com.policyflow.claimsservice.audit.ReplayAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReplayAuditRepository
        extends JpaRepository<ReplayAudit, Long> {
}