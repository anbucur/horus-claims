# HORUS-CLAIMS — BRAIN FOR SHIP INSURANCE CLAIMS

## WHAT THIS

Marine/specialty insurance claims platform. Automate FNOL intake: email → extract → deduplicate → verify policy → image forensics → human review → core system commit.

## MODULES

| Module | Tech | Job |
|--------|------|-----|
| `claims-domain` | Java 21 + JPA | Shared entities, DTOs, enums. No logic — just shapes. |
| `claims-api` | Spring Boot 4.0 + Java 21 | REST API, business logic, AI orchestration, sync engine |
| `claims-frontend` | React 18 + Vite + TypeScript | 9-screen SPA |
| `claims-ingest` | Python 3.11 + FastAPI | Email/doc ingestion → Kafka producer |
| `claims-docker` | Docker + Nginx | Infra, reverse proxy, compose orchestration |

## TECH STACK

**Backend (claims-api)**
- Spring Boot 4.0.5, Spring Data JPA, Spring Kafka, Spring WebSocket
- PostgreSQL 16 (Flyway 10.13.0 migrations — 7 scripts V1–V7)
- Valkey 8 / Redis (distributed cache) + Caffeine 3.1.8 (in-process cache)
- Kafka (Confluent 7.6.0)
- Azure AI Foundry SDK 2.0.0-beta.3, Azure OpenAI 1.0.0-beta.16, Azure Document Intelligence 1.0.7
- Resilience4j 2.2.0 (circuit breaker + retry)
- iText 7.2.5 (PDF settlement letters)
- SpringDoc OpenAPI 2.3.0 (Swagger at `/swagger-ui.html`)
- Spring Actuator (`/actuator/health`, `/metrics`, `/prometheus`)

**Frontend (claims-frontend)**
- React 18.3.1 + Vite 5.3.1 + TypeScript 5.4.5
- TanStack Table 8.17.3 (sortable/filterable data grids)
- Tailwind CSS 3.4.4
- React Router DOM 6.23.1
- Axios 1.7.2
- @21st-dev/magic 0.1.0 (AI component gen — see MCP section)

**Ingest (claims-ingest)**
- Python 3.11, FastAPI 0.109.2, Uvicorn 0.27.1
- Kafka-python 2.0.2, Psycopg2 2.9.9, Redis 5.0.1, Pydantic 2.6.1

## API BASE URL
`http://localhost:28080` (local dev via Nginx)
Spring Boot internal: `http://localhost:8080`

## API CONTROLLERS (12)

| Controller | Key Endpoints |
|-----------|---------------|
| ClaimController | GET /api/claims, /api/claims/{id}, /api/claims/summary |
| ClaimWorkflowController | POST /api/claims/{id}/workflow/*, PUT /api/claims/{id}/status |
| FnolController | POST /api/fnol/intake |
| DocumentExtractionController | POST /api/extraction/extract, /api/extraction/schema |
| ClaimReviewController | POST /api/reviews, PUT /api/reviews/{id} |
| ClaimSettlementController | POST /api/settlement/{id}, POST /api/settlement/{id}/approve |
| EvidenceController | GET/POST /api/claims/{id}/evidence |
| PartyController | GET/POST /api/parties |
| PolicyController | GET /api/policies/{id} |
| SyncController | GET /api/sync/triggers, POST /api/sync/triggers/{id}/run |
| IngestController | POST /api/ingest/email |
| HealthController | GET /api/health |

**WebSocket:** `/ws/claims/{id}` — real-time claim status updates

## KAFKA TOPICS

```
claims-fnohl-ingest        ← email FNOL docs
claims-validation          ← validation events
claims-audit               ← audit trail
claims-sync-trigger        ← sync orchestration
claims-extraction-results  ← AI extraction output
claims-settlement-events   ← settlement actions
```

## DATABASE (PostgreSQL 16)

Key tables: `claim`, `policy`, `party`, `financials`, `evidence`, `claim_status_history`, `claim_review`, `settlement_action`, `subject_matter_insured`, `target_system`, `sync_trigger`, `sync_event`, `claim_sync_state`, `field_mapping`, `settlement`

`metadata` and `config` columns use JSONB. 26 indexes. Audit trail is append-only.

## WORKFLOW STATE MACHINE

```
RECEIVED → EXTRACTING → VERIFYING → ENTITY_MATCHING → FORENSICS
        → DUPLICATE_CHECK → HITL_REVIEW → STP / REJECTED → COMPLETED
```

State machine: `ClaimWorkflowStateMachine`. Guards invalid transitions. All changes logged to `claim_status_history`.

## AI PIPELINE

**Processing Modes:**
- `AI_ASSISTED` — default; use Azure AI
- `FULL_MANUAL` — skip AI, route all to HITL
- `HYBRID` — mix AI + manual
- `AI_FALLBACK` — try AI, fall back to FULL_MANUAL if Azure down

**AI Services:**
- `DefaultAIOrchestrationService` → routes to `RealAzureAIClient` or `StubAIOrchestrationService`
- `RealAzureAIClient` → Azure AI Foundry (GPT-4o)
- `DocumentExtractionService` → Azure Document Intelligence (prebuilt-layout model)
- `LLMSchemaExtractor` → GPT-4o for structured extraction from unstructured docs
- `AICircuitBreaker` → 3 failures → auto-switch to FULL_MANUAL
- Toggle via: `claims.azure.ai.enabled`

**Duplicate Detection:** `TextSimilarityDuplicateDetector` — embedding/keyword similarity → routes to HITL if threshold exceeded

## KEY SERVICE PATTERNS

- **ClaimProcessingService**: 5-step pipeline — extract → verify → match entities → forensics → dedup → route
- **SyncEngine**: Bidirectional Guidewire sync (field mappings via JSONB `transformation_script`)
- **SyncScheduler**: Cron-based triggers from `target_system.schedule_cron`
- **SettlementLetterService**: iText PDF generation
- **ClaimAuditService**: Append-only audit trail

## DOMAIN EVENTS (Kafka/Spring)

`ClaimSubmittedEvent` → `ClaimApprovedSTPCEvent` → `ClaimRoutedToHITLEvent` → `ClaimStepCompletedEvent` → `ClaimSettledEvent`

STP approval triggers `SyncEngine` → posts to Guidewire.

## FRONTEND SCREENS (9)

- **Dashboard** — claims queue, status badges, KPIs, sync sidebar
- **ClaimOverview** — FNOL details, editable fields
- **Inbox** — email thread view for claim comms
- **Duplicates** — duplicate detection results
- **Entities** — party/policy/insured browser
- **Forensics** — image forensics analysis
- **HITLConsole** — human-in-the-loop review queue
- **SyncQueue** — core system sync status
- **TargetConfig** — target system config

## DESIGN DIRECTION

- Enterprise SaaS. Dense. Professional.
- Marine insurance / Lloyd's market aesthetic.
- Status badges: color = workflow stage.

## INFRASTRUCTURE

```
Nginx :8082 (entry)
  /api/*  → Spring Boot :8080
  /*      → Nginx frontend :80 (SPA fallback)

Services:
  postgres:16-alpine    :5432   512M limit
  kafka (Confluent 7.6) :9092   512M limit
  zookeeper             :2181   256M limit
  valkey:8-alpine       :6379
  api (Spring Boot)     :8080   1024M limit
  frontend (Nginx SPA)  :3002   256M limit
```

Env vars: `DB_PASSWORD`, `REDIS_PASSWORD` from `.env`. Spring profiles: `local` / `docker`.

## MCP — 21ST.DEV MAGIC (UI COMPONENT GEN)

Package: `@21st-dev/magic`
Generate polished UI from words. Returns variants. Use `/ui` command.
Example: `/ui create a data table with sorting and filters`
Key: get from https://21st.dev/magic → API Keys

Add to `openclaw.json`:
```json
"21st-dev-magic": {
  "command": "npx",
  "args": ["-y", "@21st-dev/magic"],
  "env": {
    "API_KEY": "your-21st-dev-api-key"
  }
}
```
