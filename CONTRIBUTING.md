# Contributing

## Branch Strategy
- `main` — production-ready code
- `feature/*` — new features
- `fix/*` — bug fixes
- `phase/*` — phase-specific work (e.g. `phase/2-ingestion`)

## Commit Messages
Format: `<type>(<scope>): <description>`

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

Examples:
- `feat(claims-api): add policy verification endpoint`
- `fix(domain): correct ClaimParty join column name`
- `docs(readme): add quick start section`

## Pre-commit
```bash
# Java (from claims-api/)
mvn compile

# Frontend (from claims-frontend/)
npm run build

# Run all tests
mvn test
```

## Adding Entities
1. Add entity class to `claims-domain/src/main/java/com/msig/claimsdomain/entities/`
2. Add Flyway migration to `claims-api/src/main/resources/db/migration/`
3. Add repository to `claims-api/src/main/java/com/msig/claimsapi/repository/`
4. Add service layer if business logic needed
5. Add controller endpoints
6. Write tests in `claims-api/src/test/java/`

## Kafka Topics
| Topic | Purpose |
|-------|---------|
| `claims-fnohl-ingest` | Inbound FNOL emails/documents |
| `claims-validation` | Validation results |
| `claims-audit` | Audit trail events |

## Coding Standards
- Java: Google Style Guide, Lombok for boilerplate
- React: Functional components, TypeScript strict mode
- CSS: Tailwind utility classes, Inter font with tabular numerals for all numeric data
