# HORUS Claims Platform Architecture

This document describes the current architecture with the main components, their connections, and each component's usage/purpose.

## 1) End-to-end architecture (ASCII)

```text
                                           External Inputs
                                  (Emails / FNOL / Documents / Images)
                                                     |
                                                     v
                                      +-----------------------------+
                                      | claims-ingest (FastAPI)     |
                                      | Purpose: intake + normalize |
                                      | Usage: publish ingest event |
                                      +--------------+--------------+
                                                     |
                                                     | Kafka: claims-fnohl-ingest
                                                     v
+-------------------------------+         +----------+----------------------------+         +------------------------------+
| claims-frontend (React SPA)   | <-----> | claims-api (Java 21 / Spring Boot)  | <-----> | External core systems         |
| Purpose: operator UI          |  REST   | Purpose: core business workflows     |  REST   | Purpose: downstream sync      |
| Usage: dashboard + HITL + ops |         | Usage: claim processing + sync       |         | Usage: commit approved claims |
+---------------+---------------+         +----------+--------------+-------------+         +------------------------------+
                |                                       |              |
                | HTTP                                  | JPA          | cache/session/state
                v                                       v              v
      +---------------------+                   +--------------+   +---------------+
      | nginx reverse proxy |                   | PostgreSQL 16|   | Valkey (Redis)|
      | Purpose: routing    |                   | Purpose: SoT |   | Purpose: cache|
      +---------------------+                   +--------------+   +---------------+
                                                     ^
                                                     |
                                                     | Flyway migrations
                                                     |
                                            +--------+---------+
                                            | db/migration SQL |
                                            +------------------+

Kafka/Event Side Channels:
  claims-api <------------------------------> Kafka (Confluent)
    - consume ingest
    - publish validation, status, audit, settled events
```

## 2) Component inventory (purpose + usage + connections)

| Component | Tech | Purpose | Typical usage | Connected to |
|---|---|---|---|---|
| `claims-frontend` | React 18 + TypeScript + Vite | User interface for claims operations | Dashboard, inbox, entities, forensics, duplicates, HITL review, settings | `nginx`, `claims-api` |
| `claims-docker/nginx` | nginx | Reverse proxy and routing entrypoint | Route browser/API traffic in containerized setup | `claims-frontend`, `claims-api` |
| `claims-api` | Spring Boot 4.0.5 / Java 21 | Core domain and workflow orchestration | REST endpoints, claim lifecycle transitions, sync event generation | PostgreSQL, Kafka, Valkey, frontend, external systems |
| `claims-domain` | Java module | Shared domain entities/enums/models | JPA entities and DTO contracts reused by API | `claims-api` |
| `claims-ingest` | FastAPI / Python 3.11 | Ingestion adapter service | Ingest external claim notifications and documents, publish events | Kafka |
| PostgreSQL | Postgres 16 | System of record | Persist policy/claim/evidence/review/sync state | `claims-api`, Flyway |
| Flyway migrations | SQL migration files | Versioned schema control | Deterministic schema changes, startup validation | PostgreSQL, `claims-api` |
| Kafka | Confluent Kafka 7.6 | Event transport between services | Ingest, validation, status, audit, settlement events | `claims-ingest`, `claims-api` |
| Valkey | Valkey 8 | Cache/low-latency data access | Caching and transient API-side state | `claims-api` |
| Sync Engine (`claims-api`) | Spring service/scheduler | Outbound integration orchestration | Queue, retry, and map approved claims to external targets | External core systems, PostgreSQL |
| GitHub Actions CI | GitHub workflows | Build/quality automation | Compile, test, frontend checks, docker build/push | repo modules |

## 3) Connection map (who talks to whom)

1. **External channels -> `claims-ingest`**
   - Intake of FNOL, docs, and metadata.
2. **`claims-ingest` -> Kafka (`claims-fnohl-ingest`)**
   - Publishes normalized ingest payloads.
3. **Kafka -> `claims-api`**
   - API consumes ingest payloads and starts workflow processing.
4. **`claims-frontend` <-> `claims-api` (REST over HTTP)**
   - UI reads and updates claims/work queues.
5. **`claims-api` <-> PostgreSQL**
   - Writes authoritative workflow and claim data.
6. **`claims-api` <-> Valkey**
   - Uses cache for faster repeated reads/transient values.
7. **`claims-api` -> external core systems**
   - Sync engine pushes approved/settled claims via configurable target systems.
8. **`claims-api` <-> Kafka**
   - Publishes status/validation/audit/settlement events.

## 4) Kafka topic purpose map

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `claims-fnohl-ingest` | `claims-ingest` | `claims-api` | Inbound FNOL + document event |
| `claims-validation` | `claims-api` | `claims-ingest` | Validation feedback loop |
| `claims-audit` | `claims-api`, `claims-ingest` | (audit downstream/ops) | Audit trail event stream |
| `claims-status` | `claims-api` | UI/ops integrations | Workflow status changes |
| `claims-settled` | `claims-api` | downstream integrations | Settlement completion events |

## 5) Usage/purpose by layer

- **Presentation layer (`claims-frontend`, nginx):**
  - Used by adjusters/ops teams to monitor and process claims.
- **Application layer (`claims-api`):**
  - Main business engine and workflow state transitions.
- **Domain layer (`claims-domain`):**
  - Canonical model definitions and shared enums.
- **Ingestion layer (`claims-ingest`):**
  - Adapter that translates external inbound data into internal events.
- **Data layer (PostgreSQL + Flyway + Valkey):**
  - Durable record + versioned schema + performance cache.
- **Integration/event layer (Kafka + Sync Engine):**
  - Decoupled inter-service messaging and external system synchronization.
- **Delivery/operations layer (Docker Compose + GitHub Actions):**
  - Local runtime composition and CI quality gates.
