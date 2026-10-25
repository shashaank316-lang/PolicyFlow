# Local Development and Operations

All commands below are intended for **Mac Terminal**, not for the `psql` prompt. Start from the repository root unless a command explicitly changes directory.

## 1. Start and inspect the stack

```bash
cd ~/Projects/PolicyFlow
docker compose up -d --build
docker compose ps
```

Follow a service's logs:

```bash
docker compose logs --tail=100 policy-service
docker compose logs --tail=100 customer-service
docker compose logs --tail=100 claims-service
docker compose logs --tail=100 kafka
docker compose logs --tail=100 postgres
```

Follow claims logs continuously:

```bash
docker compose logs -f claims-service
```

Press `Ctrl+C` to stop following logs; that does not stop the service.

## 2. Health and API smoke checks

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8081/actuator/health
curl -i http://localhost:8082/actuator/health
curl -i http://localhost:8080/api/policies
curl -i http://localhost:8081/api/customers
curl -i http://localhost:8082/api/claims
```

A healthy service responds with HTTP 200 and health JSON containing `"status":"UP"`. API responses depend on current data and local authentication configuration.

## 3. Create an end-to-end test claim

Use a unique `claimNumber`, for example `CLM-2026-DOCS-002` (change it if already used):

```bash
curl -i -X POST http://localhost:8082/api/claims \
  -H 'Content-Type: application/json' \
  -d '{
    "claimNumber": "CLM-2026-DOCS-002",
    "policyNumber": "POL-2026-1004",
    "customerId": "CUS-10001",
    "claimType": "AUTO_ACCIDENT",
    "claimAmount": 650.00
  }'
```

Then list claims:

```bash
curl -i http://localhost:8082/api/claims
```

## 4. Inspect the PostgreSQL databases

The container uses the `policyflow` database role. The local setup did not have a PostgreSQL role named `postgres`, so these commands specify the configured role explicitly.

List tables in the claims database:

```bash
docker exec -it policyflow-postgres psql -U policyflow -d policyflow_claims -c '\dt'
```

Inspect recent claims:

```bash
docker exec -it policyflow-postgres psql -U policyflow -d policyflow_claims -c "SELECT id, claim_number, policy_number, customer_id, claim_type, claim_amount, status FROM claims ORDER BY id DESC LIMIT 10;"
```

Inspect outbox state:

```bash
docker exec -it policyflow-postgres psql -U policyflow -d policyflow_claims -c "SELECT id, event_id, event_type, aggregate_id, status, published_at FROM outbox_events ORDER BY id DESC LIMIT 10;"
```

Inspect processed events:

```bash
docker exec -it policyflow-postgres psql -U policyflow -d policyflow_claims -c "SELECT event_id, event_type, processed_at FROM processed_events ORDER BY id DESC LIMIT 10;"
```

The `aggregate_id` is the claim number in this flow. Match `outbox_events.event_id` with `processed_events.event_id` to confirm the same event was consumed. If the exact column names differ in a future migration, inspect the current schema with `\d claims`, `\d outbox_events`, or `\d processed_events`.

## 5. Run the claims-service tests

```bash
cd ~/Projects/PolicyFlow/claims-service
mvn test
```

Return to the root with `cd ..` if needed. Inspect reports under `target/surefire-reports/` after tests run.

## 6. Frontend

The Vite frontend is under `react-frontend`. Start it outside Docker if that is how the current repository is configured:

```bash
cd ~/Projects/PolicyFlow/react-frontend
npm install
npm run dev
```

Use the URL printed by Vite (observed locally as `http://localhost:5173`). Use a separate Terminal tab/window if the backend containers are already running. Stop the Vite process with `Ctrl+C`.

If the repository runs the frontend in Docker instead, follow the current Compose file rather than launching a second instance.

## 7. Container lifecycle and data safety

```bash
# Stop containers; preserves data volumes
docker compose stop

# Start existing containers
docker compose start

# Remove containers and network, but normally keep named volumes
docker compose down

# Destructive to Compose-managed volumes; only run if you intend to wipe local DB/Kafka state
# docker compose down -v
```

Do not run destructive reset commands before collecting any data or logs needed for debugging.

## 8. Common errors

| Symptom | Likely area to inspect |
|---|---|
| `Failed to configure a DataSource` | datasource URL, driver, DB environment variables, database readiness, service profile |
| HTTP 401/403 | `SecurityConfig`, configured local credentials, request headers, CORS/auth boundary |
| Browser CORS failure while `curl` works | CORS config in the target service, allowed origin for the Vite URL, preflight request handling |
| Kafka connection errors | `docker compose ps`, Kafka health, internal bootstrap address (`kafka:9092` in Docker), publisher logs |
| A new outbox row remains `PENDING` | outbox publisher logs, Kafka availability, serialization/publish exceptions |
| Outbox row is `PUBLISHED` but no processed-event row | consumer group/consumer logs, topic name, deserialization, listener error handling |
| `FATAL: role "postgres" does not exist` | use `-U policyflow` for the configured local database role |
| Port already allocated | identify process/container holding that port before changing ports or stopping another project |

## 9. Environment and secret handling

Do not commit `.env` files, passwords, API tokens, private certificates, or resolved Compose output. `docker compose config` expands environment variables and can display secrets; inspect its output privately and do not attach it to public issues or commit it. Keep local secrets in an ignored local environment file or a secret manager, and ensure the Compose configuration uses variable references rather than hard-coded secret values.
