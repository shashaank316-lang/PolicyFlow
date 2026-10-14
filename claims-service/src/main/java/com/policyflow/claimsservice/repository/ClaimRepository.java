package com.policyflow.claimsservice.repository;

import com.policyflow.claimsservice.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
}