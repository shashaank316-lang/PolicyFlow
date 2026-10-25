# Architecture and Event Flow

## 1. Main building blocks

### Policy service
Spring Boot REST service on local port `8080`. It owns policy data in the `policyflow` database and exposes the policy API. The claims service calls it to validate a policy number before a claim is created. This is synchronous REST communication: a policy validation failure can stop the claim request before persistence.

### Customer service
Spring Boot REST service on local port `8081`. It owns customer data in the `policyflow_customer` database. The observed sample data included customer `CUS-10001` (John Smith). Claims currently carry a `customerId`; do not assume every customer lifecycle operation is implemented unless the current source confirms it.

### Claims service
Spring Boot REST service on local port `8082`. It owns claims in `policyflow_claims`. It coordinates policy validation, persists a claim, records an outbox event, and publishes the event asynchronously.

### PostgreSQL
A shared PostgreSQL container hosts separate logical databases for each service. Separate databases support clearer service ownership than having every service freely write to one shared schema. It is still one local database server/container in this development setup, not a separately operated database cluster for every service.

### Kafka
Kafka provides asynchronous messaging. The topic used by the outbox publisher/consumer is `claim-events`. In Docker Compose, the applications use `kafka:9092`; host-side tools use the published port `9095`.

### React / Vite
The development frontend is in `react-frontend` and runs on port `5173`. It was used to verify it could reach the backend and received HTTP 200 during the most recent reported check. Treat the UI as a simple development frontend, not a finished product UI.

## 2. Claim creation: synchronous path

1. A client sends `POST /api/claims` with claim number, policy number, customer ID, claim type, and amount.
2. The claims API receives the request and enters the claim creation service method.
3. The service calls `PolicyClient.validatePolicy(policyNumber)` to validate that the policy is acceptable. This is a synchronous HTTP/service-to-service call.
4. The new claim is assigned `SUBMITTED` status.
5. The claim is saved through `ClaimRepository`.
6. The service creates a `ClaimCreatedEvent`, including a unique event ID and claim details.
7. The event payload is serialized and saved as an `OutboxEvent` with a `PENDING` status.
8. The method returns the saved claim; the database transaction commits only if the claim and outbox writes both succeed.

The important boundary is the transaction. If outbox persistence fails, the claim write must roll back too. This was tested by forcing outbox save failure and verifying that no claim remained committed.

## 3. Asynchronous outbox-to-Kafka path

1. `OutboxPublisher` runs on a schedule (observed interval: approximately five seconds).
2. It asks `OutboxEventRepository` for up to 100 events with status `PENDING`, ordered by ID ascending.
3. For each event, it sends the stored payload to Kafka topic `claim-events` using the aggregate ID (the claim number) as the message key.
4. After the producer send succeeds, the publisher marks the row `PUBLISHED` and records `published_at`.
5. If the send fails, the publisher catches the failure and leaves the event pending for a later retry.

This outbox table is the durable bridge between the database transaction and the asynchronous broker. The DB transaction does not attempt to include the Kafka broker in the same ACID transaction.

## 4. Consumer idempotency

The Kafka consumer processes `CLAIM_CREATED` events and records the event ID in `processed_events`. The event ID has a uniqueness constraint. If the same event is delivered again, the consumer can detect that it was already processed and avoid applying the same effect twice.

Do not equate “message published” with “business processing completed.” The local verification checked both the outbox record's `PUBLISHED` status and a corresponding event ID in `processed_events`.

## 5. Event-state model

```text
Claim API request
      |
      v
Database transaction
  +-----------------------+
  | claims row            |
  | outbox_events: PENDING|
  +-----------------------+
      |
      v
Scheduled publisher -> Kafka topic: claim-events
      | success
      v
outbox_events: PUBLISHED
      |
      v
Consumer processes event, records event_id in processed_events
```

Failure cases to understand:

- **Policy validation fails:** claim creation should fail before its persistence flow completes.
- **Claim save fails:** the transaction fails and no outbox event should be committed.
- **Outbox save fails:** the transaction rolls back the claim as well.
- **Kafka send fails after transaction commit:** the claim and pending outbox event remain in the database; publisher retries later.
- **Consumer receives a duplicate:** the processed-event record protects against duplicate effects.
- **Publisher crashes after Kafka accepted a message but before the outbox row is marked PUBLISHED:** the event can be sent again during recovery, so the consumer-side idempotency protection remains important.

## 6. Data model / persistence tables

The local `policyflow_claims` database contained these tables at the time of verification:

- `claims` — claim records.
- `outbox_events` — durable event records, including event ID, event type, aggregate ID, payload, status, and publication timestamp.
- `processed_events` — event IDs already handled by the consumer, with event type and processing timestamp.
- `replay_audit` — an audit-related table present in the schema; the precise runtime workflow should be confirmed from current source before claiming how it is used.
- `flyway_schema_history` — Flyway migration history.

The `Claim` entity includes fields such as `id`, `claimNumber`, `policyNumber`, `customerId`, `claimType`, `claimAmount` (`BigDecimal`), and `status`. Claim numbers are unique. The processed event table has a unique constraint on `event_id`.

## 7. Why these patterns matter

- **Microservices:** separate APIs and data ownership make responsibilities clearer, but introduce network failure and operational complexity.
- **Synchronous REST:** useful when claim acceptance depends immediately on policy validation; it couples the request to policy-service availability and latency.
- **Asynchronous Kafka:** decouples event consumers from the claim request and supports scalable event-driven workflows, but introduces eventual consistency and retry behavior.
- **Transactional outbox:** atomically stores the business change and intent to publish an event.
- **Idempotent consumer:** makes retry/duplicate delivery safer.
- **Database-per-service:** limits direct cross-service data coupling; cross-service information should be obtained through APIs or events, not by querying another service's tables directly.
