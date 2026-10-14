
package com.policyflow.claimsservice.service;

import com.policyflow.claimsservice.client.PolicyClient;
import com.policyflow.claimsservice.entity.Claim;
import com.policyflow.claimsservice.event.ClaimCreatedEvent;
import com.policyflow.claimsservice.repository.ClaimRepository;
import com.policyflow.claimsservice.repository.OutboxEventRepository;
import com.policyflow.claimsservice.outbox.OutboxEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyClient policyClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public ClaimService(
            ClaimRepository claimRepository,
            PolicyClient policyClient,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.claimRepository = claimRepository;
        this.policyClient = policyClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    public Claim getClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Claim not found: " + id));
    }

    @Transactional
    public Claim createClaim(Claim claim) {

        // Step 1: Validate the policy before changing the database.
        policyClient.validatePolicy(claim.getPolicyNumber());

        // Step 2: Set the initial claim status.
        claim.setStatus("SUBMITTED");

        // Step 3: Save the claim inside the transaction.
        Claim savedClaim = claimRepository.save(claim);

        // Step 4: Build the event with a unique event ID.
        ClaimCreatedEvent event = new ClaimCreatedEvent(
                UUID.randomUUID().toString(),
                "CLAIM_CREATED",
                savedClaim.getClaimNumber(),
                savedClaim.getPolicyNumber(),
                savedClaim.getCustomerId(),
                savedClaim.getClaimType(),
                savedClaim.getClaimAmount(),
                Instant.now()
        );

        // Step 5: Serialize the event to JSON for the outbox.
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not serialize claim event", exception);
        }

        // Step 6: Save the outbox record in the same transaction.
        OutboxEvent outboxEvent = new OutboxEvent(
                event.eventId(),
                event.eventType(),
                event.claimNumber(),
                payload
        );

        outboxEventRepository.save(outboxEvent);

        // Step 7: Commit both database changes when the method succeeds.
        return savedClaim;
    }
}
