package com.policyflow.claimsservice.audit;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "replay_audit")
public class ReplayAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String eventId;

    @Column(length = 100)
    private String eventType;

    @Column(nullable = false, length = 30)
    private String outcome;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private Instant attemptedAt;

    protected ReplayAudit() {
    }

    public ReplayAudit(
            String eventId,
            String eventType,
            String outcome,
            String reason) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.outcome = outcome;
        this.reason = reason;
        this.attemptedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
