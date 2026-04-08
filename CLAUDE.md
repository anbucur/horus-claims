# CLAUDE.md — HORUS Claims Processing Platform

## Project Overview

MSIG Specialty Marine — Cognitive Claims Processing Platform. Automates the full marine/specialty insurance claims lifecycle:  
**Email ingestion → document extraction → duplicate detection → policy verification → image forensics → HITL review → core system commit**

Serves MGAs and carriers handling Ocean Hull, Inland Marine, P&I, Cargo, and Specie product lines.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│           React 18 SPA (claims-frontend)        │
│  Dashboard · Inbox · Entities · Forensics       │
│  Duplicates · HITL Console · Sync · Settings    │
└───────────────────┬─────────────────────────────┘
                    │ nginx (:8082 reverse proxy)
┌───────────────────▼─────────────────────────────┐
│     Spring Boot 4.0.5 REST API (claims-api)     │
│  Controllers · Services · JPA Repos · Kafka     │
│  Flyway Migrations · Redis Cache · AI Pipeline  │
└──────┬──────────────┬──────────────┬────────────┘
       │              │              │
┌──────▼──┐    ┌──────▼──┐    ┌──────▼──┐
│PostgreSQL│    │ Kafka   │    │ Valkey  │
│   16     │    │ 7.6.0   │    │  8.0    │
│(primary) │    │(events) │    │(cache)  │
└──────────┘    └────┬────┘    └─────────┘
                     │
        ┌────────────▼────────────┐
        │  FastAPI Ingest Service │
        │    (claims-ingest)      │
        │   Python 3.11 · Kafka   │
        └─────────────────────────┘
```

---

## Module Structure

| Module | Technology | Role |
|--------|-----------|------|
| `claims-domain` | Java 21 | JPA entities, DTOs, enums — shared library |
| `claims-api` | Spring Boot 4.0.5 / Java 21 | REST API, business logic, event orchestration |
| `claims-ingest` | Python 3.11 / FastAPI | Email/document ingestion, Kafka producers |
| `claims-frontend` | React 18 / TypeScript / Vite 5 | SPA with 10+ screens |

Maven root: `com.msig:horus-claims:1.0.0-SNAPSHOT` (parent POM aggregates domain, api, ingest)

---

## Tech Versions

- **Java**: 21 (release level 21)
- **Spring Boot**: 4.0.5
- **Flyway**: 10.13.0
- **Lombok**: 1.18.38
- **Jakarta Persistence**: 3.2.0
- **Resilience4j**: 2.2.0
- **Python**: 3.11
- **FastAPI**: latest (pinned in `claims-ingest/requirements.txt`)
- **React**: 18.3.1
- **TypeScript**: 5.4.5
- **Vite**: 5.3.1
- **TanStack Table**: 8.17.3
- **axios**: 1.7.2
- **TailwindCSS**: 3.4.4
- **PostgreSQL**: 16
- **Kafka**: Confluent 7.6.0
- **Valkey/Redis**: 8.0

---

## Development Commands

### Infrastructure (Docker)
```bash
# Start only infrastructure services
docker compose up -d postgres kafka valkey zookeeper

# Full stack with build
docker compose up --build

# Tear down
docker compose down
```

### Java API (`claims-api`)
```bash
# From repo root — build all Java modules
mvn compile

# Run tests
mvn test

# Start API locally (requires infra running)
cd claims-api && mvn spring-boot:run

# Build fat JAR
mvn package -DskipTests
```

### Frontend (`claims-frontend`)
```bash
cd claims-frontend

npm install             # install dependencies
npm run dev             # Vite dev server (hot reload)
npm run build           # tsc + Vite production build
npm run lint            # ESLint (zero warnings policy — --max-warnings 0)
npm run preview         # preview production build locally
```

### Python Ingest (`claims-ingest`)
```bash
cd claims-ingest
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
# OR
python -m app.main
```

### Pre-commit Checks
```bash
mvn compile             # from claims-api/ — must pass
npm run build           # from claims-frontend/ — must pass
mvn test                # all Java tests must pass
```

---

## Key URLs (Local Dev)

| Service | URL |
|---------|-----|
| REST API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Actuator health | `http://localhost:8080/actuator/health` |
| Frontend (Vite dev) | `http://localhost:5173` |
| Frontend (Docker) | `http://localhost:3000` |
| nginx proxy | `http://localhost:8082` |

---

## Domain Model

### Entity Relationships
```
Policy (1) ──→ (Many) Claim
Claim ──→ Policy (ManyToOne)
Claim ──→ (Many) SubjectMatterInsured  (vessel/cargo)
Claim ──→ (Many) Financials            (indemnity/ALAE reserves)
Claim ──→ (Many) Evidence              (survey reports, images, emails)
Claim ──→ (Many) ClaimParty            (ASSURED/BROKER/CARRIER)
Claim ──→ (Many) ClaimStatusHistory    (audit trail)
Claim ──→ (Many) ClaimReview           (HITL queue)
Claim ──→ (Many) SettlementAction
```

### WorkflowStatus State Machine
```
RECEIVED → EXTRACTING → VERIFYING → HITL → STP → APPROVED → COMPLETED
                                   ↘                      ↗
                                    REJECTED ─────────────
```
Valid transitions enforced by `ClaimWorkflowStateMachine.java`. All transitions logged to `ClaimStatusHistory`.

### Key Enums (`claims-domain`)
- `WorkflowStatus` — RECEIVED, EXTRACTING, VERIFYING, HITL, STP, APPROVED, REJECTED, COMPLETED
- `ProcessingMode` — AI_ASSISTED, SEMI_AUTOMATIC, FULL_MANUAL
- `DocumentType` — SURVEY_REPORT, IMAGE, EMAIL, INVOICE
- `PartyType` — ASSURED, BROKER, CARRIER
- `SyncStatus` — PENDING, SYNCED, FAILED, SKIPPED

---

## Java Coding Conventions

### Lombok — always use these
```java
@Data                      // getters, setters, equals, hashCode, toString
@Builder                   // builder pattern
@RequiredArgsConstructor   // constructor injection (never @Autowired field injection)
@Slf4j                     // logger via log.info/log.debug/log.error
@NoArgsConstructor         // when JPA requires it alongside @Builder
@AllArgsConstructor        // when needed alongside @NoArgsConstructor
```

### JPA / Entity Patterns
- All entities live in `claims-domain/src/main/java/com/msig/claimsdomain/entities/`
- Always include: `@EntityListeners(AuditingEntityListener.class)`, `@CreatedDate`, `@LastModifiedDate`
- IDs: `@GeneratedValue(strategy = GenerationType.IDENTITY)` (maps to BIGSERIAL)
- Money: `BigDecimal` fields with `DECIMAL(19,2)` in DB
- **NEVER** set `ddl-auto: create/update` — schema is managed exclusively by Flyway
- `open-in-view: false` — avoid lazy-loading outside transactions

### Spring Service Pattern
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional   // at class or method level as appropriate
public class MyService {
    private final MyRepository myRepository;
    // ...
}
```

### Spring Event Publishing
```java
// Publishing
applicationEventPublisher.publishEvent(new ClaimSubmittedEvent(this, claimId));

// Listening
@EventListener
public void handleClaimSubmitted(ClaimSubmittedEvent event) { ... }
```

### REST Controllers
```java
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Tag(name = "Resource", description = "Resource management endpoints")
public class ResourceController {
    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<ResourceDto> getById(@PathVariable Long id) { ... }
}
```

### Repository Pattern
```java
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    @Query("SELECT c FROM Claim c WHERE c.workflowStatus = :status")
    List<Claim> findByWorkflowStatus(@Param("status") WorkflowStatus status);
}
```

### Error Handling
- Global `@RestControllerAdvice` handles exceptions — see `GlobalExceptionHandler`
- Do not swallow exceptions; let them propagate or throw domain-specific exceptions

---

## Frontend Conventions

### Component Structure
- **Functional components only** — no class components
- All screens in `claims-frontend/src/screens/` (one file per screen)
- Shared components in `claims-frontend/src/screens/components/` or `src/components/`
- `Layout.tsx` provides sidebar nav + breadcrumb header + `<Outlet>` for nested routes

### API Layer
- All HTTP calls go through `src/api/client.ts` (axios instance with base URL and interceptors)
- Resource-specific files: `src/api/claims.ts`, `src/api/systems.ts`
- Always define TypeScript interfaces for all request/response shapes

### TypeScript
- **Strict mode** enabled: `"strict": true`, `noUnusedLocals`, `noFallthroughCasesInSwitch`
- All API response shapes must have explicit interfaces (no `any`)

### Styling
- **TailwindCSS utility classes only** — no custom CSS unless absolutely necessary
- Use `clsx` for conditional class composition
- **Inter font** throughout; **tabular numerals** (`font-variant-numeric: tabular-nums`) for all monetary/numeric data
- Use `@tanstack/react-table` v8 for all data grids/tables

### Routing
```tsx
// App.tsx — all routes defined here with BrowserRouter
<Routes>
  <Route path="/" element={<Layout />}>
    <Route index element={<Dashboard />} />
    <Route path="claims/:id" element={<ClaimOverview />} />
    {/* ... */}
  </Route>
</Routes>
```

---

## Python Conventions (`claims-ingest`)

- **Async throughout** — all endpoints `async def`
- **Pydantic models** for all request/response validation (BaseModel subclasses)
- **Lifespan context manager** for FastAPI startup/shutdown (Kafka/Redis setup)
- Kafka: `KafkaProducer` for publishing, `KafkaConsumer` for consuming
- Redis: Manual key-value with JSON serialization (no ORM)
- Entry point: `claims-ingest/app/main.py`

---

## Database & Migrations

### Flyway Rules
- Migration files: `claims-api/src/main/resources/db/migration/`
- Naming: `V{number}__{Description}.sql` (double underscore, e.g. `V8__Add_ClaimTags.sql`)
- **Never modify existing migrations** — always add a new versioned file
- `validate-on-migrate: true` — migrations must checksum-match or startup fails
- `baseline-on-migrate: true` — baseline set at V1

### Current Migrations
| File | Purpose |
|------|---------|
| `V1__Initial_Schema.sql` | Core tables: policy, claim, party, subject_matter_insured, financials, evidence, claim_party |
| `V2__Seed_Test_Data.sql` | Development seed data |
| `V3__Target_System_Sync.sql` | Sync engine tables |
| `V4__Seed_Sync_Config.sql` | Sync config seed |
| `V5__ClaimSettlement.sql` | Settlement tables |
| `V6__ClaimStatusHistory.sql` | Audit trail |
| `V7__ClaimReview.sql` | HITL review queue |

### Schema Patterns
- Primary keys: `BIGSERIAL` (auto-increment)
- Monetary: `DECIMAL(19,2)` → Java `BigDecimal`
- Large text: `TEXT` (incident narratives, classifications)
- Audit fields: `created_at`, `updated_at` TIMESTAMP — auto-updated via DB triggers
- Indexes: on `workflow_status`, `policy_id`, `claim_id` for query performance
- Foreign keys: `ON DELETE CASCADE` standard

---

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| `claims-fnohl-ingest` | Inbound FNOL emails and documents (3 partitions) |
| `claims-validation` | Validation results from ingest service |
| `claims-audit` | Audit trail events |
| `claims-status` | Workflow status change notifications |
| `claims-settled` | Settlement completion events |

Consumer group: `claims-api-group`

---

## Configuration & Environment Variables

All env vars have defaults in `claims-api/src/main/resources/application.yml`.  
Pattern: `${VAR_NAME:default_value}`

### Database
| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `claims` | Database name |
| `DB_USERNAME` | `claims` | DB user |
| `DB_PASSWORD` | `claims` | DB password |

### Kafka
| Variable | Default | Description |
|----------|---------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |

### Redis/Valkey
| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_HOST` | `localhost` | Redis/Valkey host |
| `REDIS_PORT` | `6379` | Redis/Valkey port |
| `REDIS_PASSWORD` | *(empty)* | Redis password |

### App Server
| Variable | Default | Description |
|----------|---------|-------------|
| `APP_PORT` | `8080` | API listen port |
| `APP_HOST` | `0.0.0.0` | API bind address |
| `SPRING_PROFILES_ACTIVE` | — | Set to `docker` in containers |

### Azure AI (optional — disabled by default)
| Variable | Default | Description |
|----------|---------|-------------|
| `AZURE_AI_PROJECT_ENDPOINT` | *(empty)* | Azure AI Foundry project URL |
| `AZURE_AI_API_KEY` | *(empty)* | Azure AI API key |
| `AZURE_OPENAI_DEPLOYMENT` | `gpt-4o` | Model deployment name |
| `AZURE_OPENAI_API_VERSION` | `2024-06-01` | API version |
| `AZURE_VISION_ENDPOINT` | *(empty)* | Azure Computer Vision |
| `AZURE_VISION_API_KEY` | *(empty)* | Vision API key |
| `AZURE_SEARCH_ENDPOINT` | *(empty)* | Azure AI Search |
| `AZURE_SEARCH_INDEX` | `claims` | Search index name |
| `DOCUMENT_INTELLIGENCE_ENDPOINT` | *(empty)* | Azure Document Intelligence |
| `DOCUMENT_INTELLIGENCE_API_KEY` | *(empty)* | Document Intelligence key |

---

## AI Processing Pipeline

### Configuration (`application.yml` under `claims:`)
```yaml
claims:
  processing:
    default-mode: AI_ASSISTED         # AI_ASSISTED | SEMI_AUTOMATIC | FULL_MANUAL
    stp-confidence-threshold: 85      # auto-approve above this %
    hitl-confidence-threshold: 60     # route to HITL between 60-85%; reject below 60%
    ai-enabled: true
    circuit-breaker:
      failure-threshold: 3            # failures before opening circuit
      reset-minutes: 2                # time before half-open retry
  azure:
    ai:
      enabled: false                  # true = RealAzureAIClient; false = MockAIClient
```

### AI Service Classes
- `AIOrchestrationService` (interface) — `service/ai/`
- `DefaultAIOrchestrationService` — full AI pipeline with Azure
- `StubAIOrchestrationService` — deterministic stub for dev/test
- `AICircuitBreaker` — wraps AI calls with Resilience4j circuit breaker
- `ReActClaimAgent` — ReAct-pattern agent for claim analysis
- `TextSimilarityDuplicateDetector` — duplicate detection without Azure

**Default**: When `claims.azure.ai.enabled=false`, `MockAIClient` is used — safe for local development with no Azure credentials.

---

## Adding New Features

### Adding a New Entity
1. Create entity in `claims-domain/src/main/java/com/msig/claimsdomain/entities/`
   - Add `@EntityListeners(AuditingEntityListener.class)`, `@CreatedDate`, `@LastModifiedDate`
   - Use Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
2. Create Flyway migration `V{next}__Add_{EntityName}.sql` in `claims-api/src/main/resources/db/migration/`
3. Create repository in `claims-api/src/main/java/com/msig/claimsapi/repository/` extending `JpaRepository<Entity, Long>`
4. Create service in `claims-api/src/main/java/com/msig/claimsapi/service/` with `@Service @RequiredArgsConstructor @Transactional`
5. Create controller in `claims-api/src/main/java/com/msig/claimsapi/controller/` with OpenAPI annotations
6. Write tests in `claims-api/src/test/java/`

### Adding a New API Endpoint
1. Add method to existing controller (preferred) or create new controller
2. Add `@Operation`, `@ApiResponse` annotations
3. Add service method for business logic
4. Add repository query if needed (use `@Query` JPQL annotation)
5. Update frontend API client in `claims-frontend/src/api/`

### Adding a Frontend Screen
1. Create screen file in `claims-frontend/src/screens/`
2. Add route in `App.tsx`
3. Add nav link in `Layout.tsx` sidebar

---

## Git Conventions

### Branch Strategy
- `main` — production-ready
- `feature/*` — new features
- `fix/*` — bug fixes
- `phase/*` — phase-specific work (e.g. `phase/2-ingestion`)

### Commit Message Format
```
<type>(<scope>): <description>
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

**Examples**:
```
feat(claims-api): add policy verification endpoint
fix(domain): correct ClaimParty join column name
docs(readme): add quick start section
refactor(frontend): extract claims table to reusable component
test(claims-api): add unit tests for FnolIntakeService
chore(docker): update postgres image to 16-alpine
```

---

## Key Package Structure

### Java (`com.msig`)
```
claims-domain/
  com.msig.claimsdomain.entities/   ← JPA entities
  com.msig.claimsdomain.model/      ← DTOs, value objects, enums

claims-api/
  com.msig.claimsapi/
    ClaimsApiApplication.java       ← @SpringBootApplication @EnableJpaAuditing
    controller/                     ← REST controllers
    service/                        ← business logic
      ai/                           ← AI orchestration, circuit breaker, agents
      audit/                        ← audit service
      settlement/                   ← settlement, completion, letter generation
      workflow/                     ← ClaimWorkflowStateMachine
    repository/                     ← Spring Data JPA repos
    config/                         ← Spring config (Kafka, OpenAPI, CORS, etc.)
    event/                          ← Spring application events
    dto/                            ← Request/Response DTOs
```

### Frontend (`claims-frontend/src/`)
```
main.tsx              ← ReactDOM.createRoot
App.tsx               ← BrowserRouter + all Routes
components/Layout.tsx ← sidebar nav + Outlet
screens/              ← one file per screen
api/
  client.ts           ← axios instance (base URL, interceptors)
  claims.ts           ← claims API functions
  systems.ts          ← target system API functions
```

---

## CI/CD (GitHub Actions)

Workflows in `.github/workflows/`:

| File | Trigger | Purpose |
|------|---------|---------|
| `ci.yml` | Push/PR to `master` | Java compile → test → Docker build & push (on merge) |
| `frontend.yml` | Changes in `claims-frontend/**` | `npm ci` → `npm run lint` → `npm run build` → `npm test --coverage` |
| `deploy.yml` | Push to `master` | Docker Compose deployment (stub) |

Docker images pushed to `horus/claims-api:latest` and `horus/claims-api:<sha>` (multi-platform: amd64 + arm64).

Frontend CI uses **Node.js 20**; Java CI uses **Java 21 + Maven**.

---

## Environment Setup

Copy `.env.example` to `.env` before first run:
```bash
cp .env.example .env
```

Key vars in `.env.example`:
- `DB_PASSWORD` — PostgreSQL password
- `VALKEY_PASSWORD` — Valkey/Redis password
- `SPRING_PROFILES` — set to `docker` for containerized deployments
- `KAFKA_BOOTSTRAP_SERVERS` — Kafka broker address
- `DATABASE_URL` — Full PostgreSQL connection URL
- Azure AI and Guidewire vars (commented out, for Phase 4/5)

---

## Regulatory Notes

- **GDPR**: PII must be redacted before any LLM calls; process data in EU Azure regions
- **DORA**: Kafka queues provide resilience fallback; circuit breaker prevents cascade failures
- Audit trail: every `WorkflowStatus` transition recorded in `ClaimStatusHistory` with timestamp and actor
