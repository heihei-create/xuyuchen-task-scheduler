# Multi-tenant Engineering Task Scheduler

A Java 17 / Spring Boot service for submitting and tracking engineering compute tasks. The repository focuses on a real state machine and failure boundaries rather than a CRUD-only demo.

## Implemented

- Tenant-scoped task resources through X-Tenant-Id plus constant-time API-key authentication
- Idempotent task creation by tenant + idempotency key
- Explicit task state transitions and stale worker event rejection
- Scheduled dispatch with per-tenant concurrency quota
- Worker lease and timeout reaper
- REST endpoints for create, list, inspect, start, finish and cancel
- Durable H2 task state by default, with Redis locking and RabbitMQ delivery adapters in Compose
- Lease recovery is based on persisted expiry, so a scheduler restart cannot strand a running slot

## Run locally

    mvn spring-boot:run

Set `SCHEDULER_TENANT_KEYS=tenant-a=local-dev-token` for a local smoke run; production must supply its own tenant keys.

Create a task:

    curl.exe -X POST http://localhost:8081/api/v1/tasks -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-a" -H "X-API-Key: local-dev-token" -d "{\"name\":\"modal-run\",\"payload\":\"{\"mesh\":\"demo\"}\",\"idempotencyKey\":\"run-001\"}"

## Tests

    mvn test

The core scheduler is self-contained in the test profile so unit tests do not need external services. Redis and RabbitMQ are wired in the Compose topology for distributed locks and durable delivery; RabbitMQ/DLQ operation should be validated in the deployment environment because Docker is not assumed on the developer workstation.
