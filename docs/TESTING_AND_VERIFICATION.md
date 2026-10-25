# Testing and Verification

This file records the latest results reported during the local development session. Re-run the checks on the current checkout before relying on them after any code or configuration changes.

## 1. Verified results

### Service health
All three actuator health endpoints returned HTTP 200 with health JSON containing `"status":"UP"` during the reported end-to-end check.

### REST API smoke checks
- `GET /api/policies` on port 8080 returned policy data.
- `GET /api/customers` on port 8081 returned customer data.
- `GET /api/claims` on port 8082 returned claims data.
- A claim creation request returned HTTP 201 and a claim in `SUBMITTED` status.

The specific test claim used during the last reported end-to-end verification was `CLM-2026-E2E-001`, with policy `POL-2026-1004`, customer `CUS-10001`, type `AUTO_ACCIDENT`, and amount `650.00`. The API returned claim ID 8 for that run. Those are historical local database values, not guaranteed data for a clean clone.

### Outbox and consumer end-to-end path
For that claim, the outbox row had:

- `event_type = CLAIM_CREATED`
- `aggregate_id = CLM-2026-E2E-001`
- `status = PUBLISHED`
- a populated `published_at` timestamp

The matching event ID also appeared in `processed_events`, with a populated `processed_at` timestamp approximately ten milliseconds after the publication timestamp. That is evidence that this specific test event was written to the outbox, published, and processed by the consumer.

### Claims-service tests
`mvn test` was reported as `BUILD SUCCESS` in the claims-service module. Reported test coverage includes:

- Kafka integration and duplicate-delivery/idempotency behavior using Testcontainers.
- Outbox publisher recovery from a simulated transient send failure.
- Database transaction rollback when saving the outbox event is forced to fail.

See actual source files and test reports in the current checkout for the precise assertions and test count.

## 2. What this does prove

- The tested REST paths worked in the local Docker environment.
- A claim could be created and returned by the claims API.
- The transactional outbox row was committed with the claim workflow.
- The scheduled publisher successfully published the tested event to Kafka.
- The corresponding consumer recorded the same event ID as processed.
- The claims-service test suite passed at the time it was run.

## 3. What this does not yet prove

- A real Kafka container stop/restart recovery scenario has not yet been reported as completed. The existing outbox recovery test simulates a producer send failure; it is not the same as taking down and restoring the actual broker.
- All modules and the frontend have not been documented as having a complete automated test suite. Only the claims-service test suite was explicitly reported passing.
- This project has not been demonstrated under production traffic, multi-replica deployments, disaster recovery, or formal security review.
- There is no evidence here of a production CI/CD pipeline, central observability platform, load test, chaos test, or formal threat model being completed.

## 4. Re-run checklist

From Mac Terminal:

```bash
cd ~/Projects/PolicyFlow
docker compose ps
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8081/actuator/health
curl -i http://localhost:8082/actuator/health
curl -i http://localhost:8080/api/policies
curl -i http://localhost:8081/api/customers
curl -i http://localhost:8082/api/claims

cd claims-service
mvn test
```

For the end-to-end event flow, create a new claim with a unique number, then query `outbox_events` and `processed_events` using the SQL in [Local Development and Operations](LOCAL_DEVELOPMENT.md).

## 5. Recommended next reliability test (after onboarding)

When you have time, conduct a controlled local-only test:

1. Record the current latest outbox rows.
2. Stop only the Kafka service with `docker compose stop kafka`.
3. Create one claim with a new unique number.
4. Confirm the claim is stored and its outbox event remains `PENDING` while the broker is down.
5. Restart Kafka with `docker compose start kafka` and wait for it to become healthy.
6. Confirm the event eventually changes to `PUBLISHED` and its ID appears in `processed_events`.

Do not remove volumes or reset the database for this test. Perform it after the first day rather than spending onboarding time on it. Ensure the claim service is configured to handle a Kafka outage without causing a failed claim request after the claim/outbox transaction commits; if the observed behavior differs, investigate before describing this as a verified recovery result.
