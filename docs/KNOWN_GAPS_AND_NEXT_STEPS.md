# Known Gaps and Next Steps

## Current assessment

The core local demonstration flow and claims-service tests have been exercised, but PolicyFlow is not a production-ready insurance application. Not every failure mode, module, or operational requirement has been verified.

## Known or unverified areas

1. **Real Kafka outage/restart recovery:** a simulated producer-send failure test passed, but a full test that stops the actual Kafka container, creates a claim, restarts Kafka, and confirms eventual processing has not been reported as completed.
2. **Coverage across all modules:** a successful `mvn test` was reported for `claims-service`. Do not imply that every module and frontend has equally complete automated coverage without running their tests.
3. **CI/CD:** no complete automated build/test/deployment pipeline is evidenced in this documentation.
4. **Production operational readiness:** production deployment, multi-instance scaling, disaster recovery, backups/restore, SLOs, load testing, alert tuning, and incident exercises have not been established.
5. **Security hardening:** local security configuration exists, but no formal security review, threat model, role/permission matrix, penetration test, or production secret-management review is evidenced here.
6. **API contracts and error handling:** verify validation, consistent error schemas, timeouts, retries, and behavior when dependent services are unavailable against the current source and requirements.
7. **Observability:** there are logs and actuator health checks, but a production-grade metrics/tracing/dashboard/alerting solution has not been demonstrated.
8. **Kafka operational guarantees:** inspect topic partitions, replication, retention, consumer group configuration, retry/DLQ behavior, and ordering requirements before making production guarantees. The local single-broker setup is for development.
9. **Replay/audit table:** `replay_audit` exists in the claims schema, but its exact operational use should be confirmed in source before describing it.
10. **Frontend maturity:** frontend connectivity was checked; full UX, accessibility, browser testing, and frontend test coverage are not established.

## Recommended order of work

### Priority 1 — reliability verification
- Run the actual Kafka stop/restart recovery scenario without deleting volumes.
- Re-run all existing tests from a clean checkout or known baseline.
- Add a short architecture decision record for the outbox and idempotent consumer patterns.

### Priority 2 — engineering quality
- Add validation/error contract tests for invalid claims, unknown policies, duplicate claim numbers, and downstream timeout behavior.
- Add CI for formatting, unit tests, integration tests, dependency scanning, and container builds.
- Add metrics and tracing for request IDs and event IDs across API, publisher, Kafka, and consumer.
- Confirm configuration is externalized and secrets are not committed.

### Priority 3 — production-quality evaluation
- Define NFRs and SLOs first, then perform load, scalability, resilience, recovery, security, and operational readiness testing.
- Evaluate retry backoff, poison-message handling/dead-letter topics, stale outbox monitoring, retention, and replay controls.
- Document release/rollback, DB migration strategy, backup/restore, and incident ownership.
