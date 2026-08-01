# Multi-tenant Engineering Task Scheduler

A Java 17 / Spring Boot service for submitting and tracking engineering compute tasks. The repository focuses on a real state machine and failure boundaries rather than a CRUD-only demo.

## Implemented

- Tenant-scoped task resources through X-Tenant-Id
- Idempotent task creation by tenant + idempotency key
- Explicit task state transitions and stale worker event rejection
- Scheduled dispatch with per-tenant concurrency quota
- Worker lease and timeout reaper
- REST endpoints for create, list, inspect, start, finish and cancel
- Docker Compose topology for app, Redis and RabbitMQ

## Run locally

    mvn spring-boot:run

Create a task:

    curl.exe -X POST http://localhost:8081/api/v1/tasks -H "Content-Type: application/json" -H "X-Tenant-Id: lab-a" -d "{\"name\":\"modal-run\",\"payload\":\"{\"mesh\":\"demo\"}\",\"idempotencyKey\":\"run-001\"}"

## Tests

    mvn test

The core scheduler is self-contained in the test profile so unit tests do not need external services. Redis and RabbitMQ are wired in the Compose topology for the persistence and delivery adapters.
