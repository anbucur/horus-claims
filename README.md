# MSIG Specialty Marine — Cognitive Claims Processing Platform

## Overview

A high-density, enterprise-grade cognitive claims processing platform for marine and specialty insurance. Built for Managing General Agents (MGAs) and carriers handling complex global product lines including Ocean Hull, Inland Marine, P&I, Cargo, and Specie.

The platform automates the claims intake lifecycle: from Outlook email ingestion → document extraction → duplicate detection → policy verification → image forensics → HITL review → core system commit.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend (SPA)                   │
│   7 screens: Dashboard · Inbox · Entities · Forensics      │
└────────────────────────┬──────────────────────────────────┘
                         │ nginx (:80 reverse proxy)
┌────────────────────────▼──────────────────────────────────┐
│              Java Spring Boot REST API (claims-api)         │
│   Controllers · Services · JPA Repositories               │
│   Kafka Consumer/Producer · Redis Cache · Flyway Migrations│
└──────┬──────────────┬──────────────┬───────────────────────┘
       │              │              │
┌──────▼──┐    ┌──────▼──┐    ┌──────▼──┐
│PostgreSQL│    │ Apache  │    │ Valkey  │
│   16     │    │ Kafka   │    │  8.0    │
│(primary) │    │         │    │(cache)  │
└──────────┘    └─────────┘    └─────────┘
```

## Modules

| Module | Technology | Description |
|--------|-----------|-------------|
| `claims-domain` | Java 21 | Domain entities, JPA mappings, shared DTOs |
| `claims-api` | Spring Boot 3.2 | REST API, business logic, event orchestration |
| `claims-ingest` | Python 3.11 / FastAPI | Email/doc ingestion, Kafka producers |
| `claims-frontend` | React 18 / Vite / TypeScript | Enterprise UI with TanStack Table |

## Domain Model

- **Policy** → One-to-Many → **Claim**
- **Claim** → Many-to-One → **Policy**, **Party** (assured/broker/carrier)
- **Claim** → One-to-Many → **SubjectMatterInsured** (vessel/cargo)
- **Claim** → One-to-Many → **Financials** (indemnity/ALAE reserves)
- **Claim** → One-to-Many → **Evidence** (survey reports, images, emails)

## Quick Start

### Prerequisites
- Java 21 (`java -version`)
- Docker & Docker Compose (`docker compose version`)
- Node.js 18+ (`node -v`)
- Maven 3.9+ (`mvn -version`)

### 1. Clone & configure
```bash
cd horus-claims
cp .env.example .env
```

### 2. Start infrastructure
```bash
docker compose up -d postgres kafka valkey
```

### 3. Run the API
```bash
cd claims-api
mvn spring-boot:run
```

### 4. Run the frontend
```bash
cd claims-frontend
npm install
npm run dev
```

### Full stack (Docker)
```bash
docker compose up --build
# API:   http://localhost:8080
# Front: http://localhost:80
```

## Future Phases

- **Phase 2** — Ingestion Layer: Azure Logic Apps email, Document Intelligence, duplicate detection
- **Phase 3** — React Frontend: All 7 screens with real API integration
- **Phase 4** — AI Pipeline: Azure AI Foundry ReAct agents, image forensics, semantic search
- **Phase 5** — Guidewire Integration: Bidirectional REST + Kafka event streaming

## Regulatory Compliance

- **GDPR**: All data processing in EU Azure regions; PII redaction before LLM calls
- **DORA**: ICT risk management, automated incident reporting, resilience fallback via Kafka queues
