package com.policyflow.claimsservice.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ClaimCreatedEvent(
        String eventId,
        String eventType,
        String claimNumber,
        String policyNumber,
        String customerId,
        String claimType,
        BigDecimal claimAmount,
        Instant eventTimestamp
) {
}
