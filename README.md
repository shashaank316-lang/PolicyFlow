# PolicyFlow

PolicyFlow is a Java and Spring Boot microservices project that models core insurance workflows involving customers, policies, and claims.

## Architecture

The application contains three services:

- **Customer Service**: Manages customer records and exposes customer APIs.
- **Policy Service**: Manages insurance policies and validates customer information through Customer Service.
- **Claims Service**: Manages claims, validates policies through Policy Service, and publishes claim events through Apache Kafka.

### Event-driven processing

Claims Service uses the transactional outbox pattern to persist claim changes and event records before asynchronous publication. Kafka consumers process claim events with duplicate-event protection. Failed events can be routed to a dead-letter topic, with an operator-oriented replay endpoint and replay audit records.

## Technology Stack

- Java 17
- Spring Boot
- Maven
- PostgreSQL
- Apache Kafka
- Docker
- Git and GitHub

## Local Service Ports

| Service | Port | Database |
|---|---:|---|
| Policy Service | 8080 | `policyflow` |
| Customer Service | 8081 | `policyflow_customer` |
| Claims Service | 8082 | `policyflow_claims` |

## Prerequisites

Install and configure:

- Java 17
- Maven
- PostgreSQL
- Apache Kafka

Docker is used for containerizing Claims Service in the current project setup.

## Configuration

Each service has an `application.properties` file under `src/main/resources`.

Claims Service supports environment-variable overrides for database connection settings, server port, and Kafka bootstrap servers. Customer and Policy Services also support environment-variable overrides for database URL, username, password, and server port.

Configure your own local environment before starting the services. Do not commit passwords, access tokens, or other secrets.

## Build and Test

Run these commands from the repository root:

```bash
./customer-service/mvnw -f customer-service/pom.xml clean test
./policy-service/mvnw -f policy-service/pom.xml clean test
mvn -f claims-service/pom.xml clean test
```

Each command should finish with `BUILD SUCCESS`.

## API Overview

- Policy Service: `http://localhost:8080`
- Customer Service: `http://localhost:8081`
- Claims Service: `http://localhost:8082`

Claims Service exposes claim APIs and an administrative dead-letter replay endpoint. Consult the controllers and configuration for the exact request formats and behavior.

## Reliability Concepts Demonstrated

- Service-to-service REST communication
- Event-driven architecture with Kafka
- Transactional outbox
- Consumer-side duplicate-event protection
- Dead-letter handling and replay auditing
- Unit and integration testing
- Containerization

## Development Workflow

Changes should be developed on feature branches, tested, pushed to GitHub, and submitted through pull requests for review before merging into `main`.