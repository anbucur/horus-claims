# HORUS Stack Review (Keep Java + Spring Boot)

## 1) Current stack (what is there today)

- **Backend core:** Java 21 + **Spring Boot 4.0.5** (`claims-api`)
- **Domain module:** shared Java entities/models (`claims-domain`)
- **Ingestion service:** Python 3.11 + FastAPI (`claims-ingest`)
- **Frontend:** React 18 + TypeScript + Vite (`claims-frontend`)
- **Data & messaging:** PostgreSQL 16, Kafka 7.6, Valkey 8
- **Infra/runtime:** Docker Compose, GitHub Actions CI
- **Schema management:** Flyway migrations
- **Observability pieces present:** Actuator + OTLP tracing dependencies

---

## 2) Current architecture (ASCII)

```text
                               External Sources
                            (email/docs/images/FNOL)
                                      |
                                      v
                         +----------------------------+
                         | claims-ingest (FastAPI)    |
                         | Python 3.11                |
                         +-------------+--------------+
                                       | Kafka topic: claims-fnohl-ingest
                                       v
+----------------------+     +-----------------------------+     +----------------------+
| claims-frontend SPA  | --> | claims-api (Spring Boot)   | --> | External core systems|
| React + TS + Vite    | <-- | Java 21, REST, workflows   | <-- | (sync engine targets)|
+----------+-----------+     +------+-----------+----------+     +----------------------+
           |                          |           |
           |                          |           +--------------------+
           |                          |                                |
           v                          v                                v
   +---------------+          +---------------+                +---------------+
   | nginx proxy   |          | PostgreSQL 16 |                | Valkey/Redis  |
   +---------------+          +---------------+                +---------------+
```

---

## 3) What can be improved (highest value first)

1. **Align architecture documentation with reality**
   - Some docs and config files can drift on version/platform details.
   - Maintain one canonical “Architecture + Runtime” doc (`ARCHITECTURE.md`) and link it from README.

2. **Fix frontend CI contract**
   - `frontend.yml` runs `npm test -- --coverage`, but `claims-frontend/package.json` has no `test` script.
   - Either add tests + script, or remove that CI step until tests exist.

3. **Fix linting readiness**
   - `npm run lint` expects ESLint config, but no config file is present.
   - Add and enforce a repo-standard ESLint config so local and CI behavior match.

4. **Reduce configuration ambiguity**
   - `claims-api/src/main/resources/application.yml` has repeated top-level `spring:` sections.
   - Consolidate to one section to avoid accidental override/confusion.

5. **Clarify service boundaries**
   - Document the “source of truth” ownership:
     - Ingest service owns intake normalization.
     - Spring API owns claim state transitions/business rules.
     - Sync engine owns outbound integration.
   - This reduces confusion about where new logic should live.

6. **Stabilize local developer experience**
   - Add a single `make`/script entrypoint for: infra up, api run, frontend run, smoke checks.
   - Keep Java/Spring Boot as core, but simplify day-1 setup.

---

## 4) Recommended target state (still Java + Spring Boot centric)

- Keep **Java + Spring Boot** as the primary business platform.
- Keep Python ingest as a dedicated adapter service (not business-rule service).
- Formalize event contracts (topic schemas, retry/dead-letter behavior, ownership).
- Add thin quality gates that always pass locally before CI (compile, lint, basic tests).
- Keep architecture docs versioned and updated with each structural change.

This preserves your current direction while making the architecture easier to understand and operate.
