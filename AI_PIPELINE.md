# AI Pipeline & Features — Horus Claims

This document describes all AI-powered pipelines, features, and components in the Cognitive Claims Processing Platform, including how each component is wired together, what Azure services are consumed, and how the system degrades gracefully when AI is unavailable.

---

## Table of Contents

1. [High-Level Architecture](#1-high-level-architecture)
2. [End-to-End Claim Processing Pipeline](#2-end-to-end-claim-processing-pipeline)
3. [AI Component Catalogue](#3-ai-component-catalogue)
   - [AIClient (interface)](#aiclient-interface)
   - [RealAzureAIClient](#realazureaiclient)
   - [MockAIClient](#mockaiclient)
   - [AIOrchestrationService (interface)](#aiorchestrationservice-interface)
   - [DefaultAIOrchestrationService](#defaultaiorchestrationservice)
   - [StubAIOrchestrationService](#stubaiorchestrationservice)
   - [ReActClaimAgent / ReActClaimAgentImpl](#reactclaimagent--reactclaimagentimpl)
   - [TextSimilarityDuplicateDetector](#textsimilarityduplicatedetector)
   - [SemanticSearchService](#semanticsearchservice)
   - [DocumentExtractionService](#documentextractionservice)
   - [LLMSchemaExtractor](#llmschemaextractor)
   - [AICircuitBreaker](#aicircuitbreaker)
   - [AICacheService](#aicacheservice)
   - [AIMetricsService](#aimetricsservice)
   - [ClaimProcessingService](#claimprocessingservice)
   - [ClaimWorkflowStateMachine](#claimworkflowstatemachine)
4. [Pipeline Step Detail](#4-pipeline-step-detail)
5. [Routing Decisions: STP / HITL / SIU](#5-routing-decisions-stp--hitl--siu)
6. [Processing Modes](#6-processing-modes)
7. [Resilience Patterns](#7-resilience-patterns)
8. [Azure Services Used](#8-azure-services-used)
9. [Configuration Reference](#9-configuration-reference)
10. [Frontend AI Screens](#10-frontend-ai-screens)

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          claims-ingest (Python / FastAPI)               │
│  Email webhook  →  Kafka producer  →  "claims-fnohl-ingest" topic       │
└──────────────────────────────────┬──────────────────────────────────────┘
                                   │ Kafka
┌──────────────────────────────────▼──────────────────────────────────────┐
│                       claims-api (Spring Boot 3.2 / Java 21)            │
│                                                                         │
│  FnolIntakeService          ─── Receives FNOL, creates Claim entity     │
│  ClaimProcessingService     ─── Orchestrates the 7-step AI pipeline     │
│  ClaimWorkflowStateMachine  ─── State transitions & confidence gates    │
│                                                                         │
│  ┌─── AI Layer ──────────────────────────────────────────────────────┐  │
│  │  AIOrchestrationService (interface)                               │  │
│  │      ├── DefaultAIOrchestrationService  [ai-enabled=true]         │  │
│  │      └── StubAIOrchestrationService     [ai-enabled=false]        │  │
│  │                                                                   │  │
│  │  AIClient (interface)                                             │  │
│  │      ├── RealAzureAIClient    [azure.ai.enabled=true]             │  │
│  │      └── MockAIClient         [azure.ai.enabled=false / default]  │  │
│  │                                                                   │  │
│  │  ReActClaimAgent (interface)                                      │  │
│  │      └── ReActClaimAgentImpl  [azure.ai.enabled=true]             │  │
│  │                                                                   │  │
│  │  SemanticSearchService   — Azure AI Search OR PostgreSQL FTS      │  │
│  │  TextSimilarityDuplicateDetector — local TF-IDF cosine similarity │  │
│  │  DocumentExtractionService  — Azure Document Intelligence OCR     │  │
│  │  LLMSchemaExtractor         — GPT-4o schema-guided extraction     │  │
│  │  AICircuitBreaker           — failure threshold + auto-reset      │  │
│  │  AICacheService             — Caffeine LRU (5-min TTL, 1,000 max) │  │
│  │  AIMetricsService           — Micrometer counters + latency timers│  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
         │ Azure AI Foundry (OpenAI Chat Completions)
         │ Azure Document Intelligence
         │ Azure AI Search
         │ Azure Computer Vision
```

---

## 2. End-to-End Claim Processing Pipeline

A claim travels through seven ordered steps driven by `ClaimProcessingService`. Each step can run in AI-assisted, semi-automatic, or full-manual mode (see [Processing Modes](#6-processing-modes)).

```
Email / API
    │
    ▼
┌──────────────┐
│  1. INGEST   │  FnolIntakeService creates Claim (status: RECEIVED)
└──────┬───────┘
       │  ClaimProcessingService.processClaim()
       ▼
┌──────────────┐
│  2. EXTRACT  │  AI reads FNOL text → structured fields (date, location,
│              │  narrative, estimated value, vessel details, IMO number)
│              │  [AIClient.extractClaimData → GPT-4o]
└──────┬───────┘
       │  claim.aiConfidenceScore updated
       ▼
┌──────────────┐
│  3. VERIFY   │  Checks policy number & date of loss against policy terms,
│              │  marine-specific warranties, exclusions, trading limits
│              │  [AIClient.verifyPolicy → GPT-4o]
└──────┬───────┘
       ▼
┌──────────────┐
│  4. ENTITY   │  Matches extracted names (assured, broker, vessel, IMO)
│   MATCHING   │  against known registries with typo tolerance
│              │  [AIClient.matchEntities → GPT-4o]
└──────┬───────┘
       ▼
┌──────────────┐
│  5. FORENSICS│  Analyses evidence image URLs:
│              │  a) Azure Computer Vision REST — metadata, flagged regions
│              │  b) GPT-4o Vision — narrative consistency check
│              │  [AIClient.runForensics → GPT-4o-vision + Computer Vision]
└──────┬───────┘
       ▼
┌──────────────┐
│  6. DUPLICATE│  Local TF-IDF cosine similarity (≥ 60 % threshold) on
│   CHECK      │  narrative + location + vessel name — no AI credential needed
│              │  [TextSimilarityDuplicateDetector]
└──────┬───────┘
       ▼
┌──────────────┐
│  7. ROUTE    │  ReAct agent (GPT-4o) synthesises all prior results
│              │  → decides STP | HITL | SIU with confidence + reasoning
│              │  [ReActClaimAgentImpl → GPT-4o]
└──────┬───────┘
       │
  ┌────┴──────────────────────────────────────┐
  │  STP (≥ 85 % confidence, no red flags)   │  → auto-approved
  │  HITL (< 85 %, or human-review required) │  → adjuster queue
  │  SIU  (deepfake / fraud signals)         │  → investigation unit
  └───────────────────────────────────────────┘
```

---

## 3. AI Component Catalogue

### AIClient (interface)

**File:** `claims-api/.../service/ai/AIClient.java`

The central abstraction over the AI provider. All AI-bound operations are called through this interface so the rest of the codebase is completely decoupled from the concrete provider.

| Method | What it does |
|---|---|
| `extractClaimData(rawText)` | Parses FNOL free text into structured fields (date, location, narrative, value, currency, confidence). Marine-specific fields: vessel name, IMO number, cargo description, voyage route, ports. |
| `verifyPolicy(policyNumber, dateOfLoss)` | Checks policy status, coverage period, line of business, marine warranties, exclusions, and trading limits. |
| `matchEntities(entityNames)` | Fuzzy-matches extracted names (company, vessel, person) against known registries. Returns matches with confidence ≥ 0.75. |
| `runForensics(imageUrls)` | Returns manipulation score, flagged regions, deepfake flag, narrative mismatch flag, and summary. |
| `findSimilarClaims(queryText, limit)` | Semantic search (delegates to `SemanticSearchService`). |
| `isAvailable()` | Returns `true` if the real AI service is reachable/configured. |
| `getModelName()` | Returns the active model name (e.g. `gpt-4o` or `mock-ai-stub`). |

Response types are Java `record`s defined inside `AIClient`: `ExtractedClaimData`, `PolicyVerificationResult`, `EntityMatch`, `ForensicsResult`.

---

### RealAzureAIClient

**File:** `claims-api/.../service/ai/RealAzureAIClient.java`  
**Activates:** `claims.azure.ai.enabled=true`

The production AI client. Uses the **Azure AI Foundry SDK** (`azure-ai-projects` + `azure-ai-openai`) to call **GPT-4o** via Chat Completions.

Key implementation details:

| Feature | Detail |
|---|---|
| **Authentication** | API key (`AzureKeyCredential`) or passwordless Entra ID (`DefaultAzureCredential`) |
| **Endpoint** | Azure AI Foundry project endpoint: `https://<resource>.services.ai.azure.com/api/projects/<project>` |
| **Model** | Configurable via `claims.azure.ai.model-deployment` (default: `gpt-4o`) |
| **Retry** | Exponential back-off, max 3 retries, 500 ms base delay |
| **Timeouts** | Connect: 5 s, read: 60 s (30 s for policy verify / entity match) |
| **Caching** | `AICacheService` wraps `extractClaimData` — same input within 5 minutes returns cached result |
| **Metrics** | Every call timed via `AIMetricsService` |
| **Forensics** | Two-pass: (a) Azure Computer Vision REST for metadata; (b) GPT-4o vision for narrative consistency |

**Prompts are marine-insurance-optimised:**
- Extraction prompt lists vessel name, IMO number, cargo, voyage route as critical fields with marine-specific normalisation rules (e.g. tolerating vessel name typos like "MSK Oscar" → "MSC OSCAR").
- Policy verification prompt checks warranty compliance, exclusions, and trading restrictions specific to marine hull & cargo policies.
- Entity matching uses tiered confidence thresholds (vessels ≥ 0.85, companies ≥ 0.80, persons ≥ 0.75).

---

### MockAIClient

**File:** `claims-api/.../service/ai/MockAIClient.java`  
**Activates:** always (unless `RealAzureAIClient` is active)

A stub that returns deterministic empty/null results for all operations. Intended for local development and CI runs without Azure credentials. All methods set `aiAvailable=false` and `aiModelUsed="mock-ai-stub"`, which causes the circuit breaker to record a failure and the orchestration layer to route the claim to HITL for manual review.

---

### AIOrchestrationService (interface)

**File:** `claims-api/.../service/ai/AIOrchestrationService.java`

Sits between `ClaimProcessingService` and `AIClient`. It translates between domain objects (`Claim`, `Policy`, `Evidence`, etc.) and the flat strings consumed by the AI client, wraps every call in the circuit breaker, maps AI responses to domain models, and returns a typed `ProcessingResult<T>`.

| Method | Domain input | AI call | Domain result |
|---|---|---|---|
| `extractClaimData` | `FNOLDocument` | `AIClient.extractClaimData` | `ExtractedClaimData` |
| `verifyPolicy` | `Claim`, `Policy` | `AIClient.verifyPolicy` | `PolicyVerificationResult` |
| `matchEntities` | `ExtractedEntities` | `AIClient.matchEntities` | `EntityMatchResult` |
| `runForensics` | `List<Evidence>` | `AIClient.runForensics` | `ForensicsResult` |
| `detectDuplicates` | `Claim` | (reserved for AI-based detection) | `List<DuplicateMatch>` |
| `findSimilarClaims` | `Claim` | `SemanticSearchService` | `List<ClaimSimilarityResult>` |
| `recommendRouting` | `Claim`, `ClaimContext` | `ReActClaimAgent` | `ClaimRecommendation` |

---

### DefaultAIOrchestrationService

**File:** `claims-api/.../service/ai/DefaultAIOrchestrationService.java`  
**Activates:** `claims.processing.ai-enabled=true` (default)

The main production implementation of `AIOrchestrationService`. Injects the active `AIClient` (real or mock), `AICircuitBreaker`, `SemanticSearchService`, and `ReActClaimAgent`.

**Circuit breaker guard:** Every public method starts with `if (circuitBreaker.isOpen()) return empty(SEMI_AUTOMATIC)` so a broken AI service never blocks claim processing.

**Entity match result mapping:** Parses the untyped AI `EntityMatch` records and builds a structured `EntityMatchResult` with assuredId, brokerId, vesselIds, and an overall confidence score.

---

### StubAIOrchestrationService

**File:** `claims-api/.../service/ai/StubAIOrchestrationService.java`  
**Activates:** `claims.processing.ai-enabled=false`

Returns empty `ProcessingResult` objects with `FULL_MANUAL` mode for all methods. Used when AI is explicitly disabled in configuration. All claims route to HITL.

---

### ReActClaimAgent / ReActClaimAgentImpl

**Files:**
- `claims-api/.../service/ai/ReActClaimAgent.java` (interface)
- `claims-api/.../service/ai/ReActClaimAgentImpl.java` (implementation)

**Activates:** `claims.azure.ai.enabled=true`

Implements a **ReAct-style (Reason + Act)** routing agent using **GPT-4o**. Given the full claim context (extracted data, policy result, entity match, forensics, duplicates, similar claims), it reasons over all signals and recommends one of three routing decisions.

**Prompt structure (`buildPrompt`):**

```
System: You are an expert marine insurance claims underwriter.
        Analyse the following claim and recommend STP | HITL | SIU.

Claim fields → AI extracted data → policy verification →
entity matching → forensics → duplicate matches → similar claims

Decision rules:
  STP  — confidence > 0.90, policy active, no forensics red flags, no duplicates
  SIU  — deepfake detected, quarantine recommended, or narrative mismatch
  HITL — all other cases (default)

Response: JSON { decision, reasoning, confidence (0–1), considerations[] }
```

**Model call:** `temperature=0.1`, `maxTokens=600` — deliberately low temperature for deterministic routing decisions.

---

### TextSimilarityDuplicateDetector

**File:** `claims-api/.../service/ai/TextSimilarityDuplicateDetector.java`

**No external AI credentials required.** Runs entirely in the JVM using TF-IDF cosine similarity.

- Builds a corpus text from `incidentNarrative + lossLocation + vesselName` for each claim.
- Computes cosine similarity between the incoming claim and all existing claims.
- Returns matches with similarity ≥ 60 % (configurable `THRESHOLD`).
- For each match, produces a human-readable `matchReason` listing the top shared terms.

This component ensures duplicate detection works even when Azure AI is unavailable.

---

### SemanticSearchService

**File:** `claims-api/.../service/SemanticSearchService.java`

Finds semantically similar past claims. Uses a two-tier approach:

1. **Azure AI Search** — if `claims.azure.ai.search-endpoint` and `claims.azure.ai.search-api-key` are set, sends a vector/keyword query to the `claims` index (configurable name) using REST API version `2024-06-01`. Returns results with `@search.score`.

2. **PostgreSQL full-text search fallback** — if Azure AI Search is not configured or fails, executes `claimRepository.findSimilarByText(query, excludeClaimId, limit)` using PostgreSQL's native FTS.

Results feed into the ReAct agent's context for routing decisions.

---

### DocumentExtractionService

**File:** `claims-api/.../service/DocumentExtractionService.java`  
**Activates:** `claims.document-intelligence.enabled=true`

Wraps the **Azure Document Intelligence SDK** (`azure-ai-documentintelligence` v1.0.7, API `2024-11-30`) to perform OCR and structured layout extraction on uploaded documents (PDF, PNG, JPG, TIFF, DOCX, XLSX, PPTX).

**Pipeline:**
1. Calls `DocumentIntelligenceClient.beginAnalyzeDocument()` with the configured model (default: `prebuilt-layout`).
2. Parses the `AnalyzeResult` into structured `RawDocumentContent`: text lines with labels (headings, field labels), tables (row × column), key-value pairs, entities, languages, and page count.
3. Converts `RawDocumentContent` to a Markdown-formatted string (`toLLMInputText`) for downstream GPT-4o consumption.

---

### LLMSchemaExtractor

**File:** `claims-api/.../service/LLMSchemaExtractor.java`  
**Activates:** `claims.azure.ai.enabled=true`

A **schema-guided extraction pipeline** that chains `DocumentExtractionService` (OCR) with GPT-4o (structured JSON extraction).

**Usage:**
```java
ExtractionSchema schema = ExtractionSchema.builder()
    .addField("dateOfLoss",       "string", "Date of the incident in ISO-8601")
    .addField("incidentNarrative","string", "Description of the incident")
    .addField("vesselName",       "string", "Name of the vessel involved")
    .build();

DocumentExtractionResult result = llmSchemaExtractor.extract(documentBytes, "claim.pdf", schema);
```

**GPT-4o response format:** Per-field objects with `value`, `confidenceScore` (0–1), `sourceTextSpan` (the exact supporting text), and an optional `warning` (`not_found` | `ambiguous` | `contradictory`). Also returns `overallConfidenceScore` and `extractionWarnings`.

Model call: `temperature=0.1`, `maxTokens=2000`.

---

### AICircuitBreaker

**File:** `claims-api/.../service/ai/AICircuitBreaker.java`

A hand-rolled three-state circuit breaker protecting all AI calls:

| State | Behaviour |
|---|---|
| `CLOSED` | Normal — all AI calls pass through |
| `OPEN` | Tripped — all AI calls return empty/fallback immediately |
| `HALF_OPEN` | Recovery — one call allowed through; success closes, failure re-opens |

**Thresholds:** Opens after **3 consecutive failures**. Auto-resets to HALF_OPEN after **120 seconds** (checked every 60 s by a `@Scheduled` task).

**Metrics:** `totalCalls`, `failedCalls`, `failureRate` exposed on the component.

---

### AICacheService

**File:** `claims-api/.../service/ai/AICacheService.java`

In-memory LRU cache for `extractClaimData` results using **Caffeine**.

| Parameter | Value |
|---|---|
| Max size | 1,000 entries |
| TTL | 5 minutes (expireAfterWrite) |
| Key | MD5 hash of the raw input text |
| Stats | Recorded via Caffeine stats (hit count, miss count, hit rate, eviction count) |

Same FNOL text submitted within 5 minutes returns a cached extraction — avoiding repeated LLM costs for identical re-submissions.

---

### AIMetricsService

**File:** `claims-api/.../service/ai/AIMetricsService.java`

Emits **Micrometer** metrics (exposed at `/actuator/prometheus`) for every AI call:

| Metric | Tags | Description |
|---|---|---|
| `ai.calls.total` | `operation` | Total calls per operation |
| `ai.calls.success` | `operation` | Successful calls |
| `ai.calls.failure` | `operation` | Failed calls |
| `ai.calls.latency` | `operation` | Latency timer (records actual duration) |

Structured log line per call:
```
[AI] operation=extractClaimData model=gpt-4o latency=1243ms success=true error=
```

The `timed(operation, model, supplier)` helper wraps any callable, measures wall-clock duration, and records success/failure automatically.

---

### ClaimProcessingService

**File:** `claims-api/.../service/ClaimProcessingService.java`

The **pipeline orchestrator**. Called by the REST controller or Kafka consumer with `(claimId, processingMode)`.

Sequence:
1. `extract(claim, mode, traceId)` — sets `claim.aiConfidenceScore`; on success → status `VERIFYING`; on failure → status `HITL`.
2. `verifyPolicy(claim, mode, traceId)` — checks policy; on success → stays `VERIFYING`.
3. `matchEntities(claim, mode, traceId)` — entity reconciliation.
4. `runForensics(claim, mode, traceId)` — image analysis; triggers quarantine warning log if `quarantineRecommended=true`.
5. `checkDuplicates(claim, mode, traceId)` — local TF-IDF check.
6. `route(claim, mode, traceId)` — final routing via `ClaimWorkflowStateMachine` and `ReActClaimAgent`.

Each step publishes a Spring `ApplicationEvent` (`ClaimStepCompletedEvent`, `ClaimRoutedToHITLEvent`, `ClaimApprovedSTPCEvent`) for audit trail integration.

All processing errors are caught, the claim is set to `HITL`, and a `ClaimAuditService` record is written.

---

### ClaimWorkflowStateMachine

**File:** `claims-api/.../service/workflow/ClaimWorkflowStateMachine.java`

Governs allowed state transitions and confidence gates:

```
RECEIVED → EXTRACTING → VERIFYING → (ENTITY_MATCHING → FORENSICS → DUPLICATE_CHECK) → HITL_REVIEW → STP → COMPLETED
                                                                                      └→ APPROVED → COMPLETED
                                                                                      └→ REJECTED → COMPLETED
```

Key rules:
- `requiresHumanReview()` — returns `true` if `FULL_MANUAL` mode or AI confidence < 60.
- `canAutoAdvance()` — only in `SEMI_AUTOMATIC` mode with confidence ≥ 85 at `DUPLICATE_CHECK`.
- `isAIPrimaryStep()` — classifies `EXTRACTING`, `VERIFYING`, `ENTITY_MATCHING`, `FORENSICS`, `DUPLICATE_CHECK` as AI steps (skipped in `FULL_MANUAL`).

**Confidence thresholds** (configured in `application.yml`):

| Threshold | Value | Effect |
|---|---|---|
| `stp-confidence-threshold` | 85 | Auto-approve as STP |
| `hitl-confidence-threshold` | 60 | Route to HITL below this |

---

## 4. Pipeline Step Detail

### Step 2 — FNOL Data Extraction

The raw FNOL text assembled from the `FNOLDocument` (policy number, date of loss, loss location, incident description) is sent to GPT-4o with a marine-optimised system prompt. The model returns a JSON object with:

- Standard fields: `dateOfLoss`, `incidentNarrative`, `lossLocation`, `estimatedValue`, `currency`, `confidenceScore`
- Marine-specific fields: `vesselName`, `imoNumber`, `cargoDescription`, `voyageRoute`, `portOfDeparture`, `portOfDestination`

The `confidenceScore` (0–1) is stored on the `Claim` entity as `aiConfidenceScore` and drives all downstream routing decisions.

### Step 5 — Image Forensics

Two independent analysis passes:

**Pass A — Computer Vision REST** (`analyzeImageViaRest`):
- Calls Azure Computer Vision (`2024-05-01-preview`) for each evidence URL.
- Returns flagged image regions and a manipulation probability score.

**Pass B — GPT-4o Vision** (`chatWithImage`):
- Sends each image URL to GPT-4o with a marine forensics prompt.
- Checks for consistency between the image content and the claim narrative (hull damage type, geographic markers, timestamp metadata).
- Returns `{ consistent: true|false, reason: "..." }`.

A `quarantineRecommended=true` result is set when: deepfake detected, narrative mismatch found, or manipulation score > 60 %.

### Step 7 — ReAct Routing

The ReAct agent receives a `ClaimContext` record aggregating all prior step results and sends a single, comprehensive prompt to GPT-4o. The model returns:

```json
{
  "decision": "STP | HITL | SIU",
  "reasoning": "...",
  "confidence": 0.0–1.0,
  "considerations": ["...", "..."]
}
```

If the JSON cannot be parsed, or any exception occurs, the default fallback is `HITL`.

---

## 5. Routing Decisions: STP / HITL / SIU

| Decision | Meaning | Trigger conditions |
|---|---|---|
| **STP** | Straight-Through Processing — auto-approved | AI confidence > 90 %, policy active, no forensics red flags, no duplicate matches |
| **HITL** | Human-in-the-Loop — adjuster review required | Confidence between 60–90 %, or any uncertainty; default fallback |
| **SIU** | Special Investigations Unit | Deepfake detected, quarantine recommended, high anomaly flags, narrative mismatch |

---

## 6. Processing Modes

Set via `claims.processing.default-mode` (default: `AI_ASSISTED`) or passed per-request.

| Mode | AI Extraction | Policy Verify | Entity Match | Forensics | Duplicate Check | Routing |
|---|---|---|---|---|---|---|
| `AI_ASSISTED` | ✅ GPT-4o | ✅ GPT-4o | ✅ GPT-4o | ✅ GPT-4o Vision | ✅ TF-IDF | ✅ ReAct |
| `SEMI_AUTOMATIC` | ✅ GPT-4o | ✅ GPT-4o | ✅ GPT-4o | ✅ GPT-4o Vision | ✅ TF-IDF | ✅ ReAct (HITL fallback) |
| `FULL_MANUAL` | ⛔ skipped | ⛔ skipped | ⛔ skipped | ⛔ skipped | ⛔ skipped | Always HITL |

In `FULL_MANUAL`, all AI primary steps are bypassed and the claim goes directly to an adjuster.

---

## 7. Resilience Patterns

### Circuit Breaker

`AICircuitBreaker` wraps all external AI calls. On 3 consecutive failures the circuit opens, blocking further AI calls for 120 seconds. After the timeout the circuit transitions to HALF_OPEN; one successful call closes it.

### Retry with Exponential Back-off

`RealAzureAIClient.withRetry()` retries transient errors (socket timeouts, HTTP 5xx) up to 3 times with 500 ms base delay (doubled each attempt). Non-transient errors (HTTP 4xx) are not retried.

### Graceful Degradation

| Scenario | Fallback |
|---|---|
| Azure AI disabled / no credentials | `MockAIClient` → `aiAvailable=false` → claim routed to HITL |
| Circuit breaker open | Empty `ProcessingResult` returned immediately → HITL |
| Azure AI Search unavailable | Fall back to PostgreSQL full-text search |
| GPT-4o call fails after retries | Empty extraction → `SEMI_AUTOMATIC` mode |
| ReAct agent fails or JSON unparseable | Hardcoded `HITL` recommendation |
| All AI disabled (`ai-enabled=false`) | `StubAIOrchestrationService` → `FULL_MANUAL` mode |

### Caching

`AICacheService` uses MD5-keyed Caffeine cache (1,000 entries, 5-min TTL) for `extractClaimData`. Identical FNOL documents submitted within the TTL return instantly without an LLM call.

---

## 8. Azure Services Used

| Service | SDK | Purpose | Feature flag |
|---|---|---|---|
| **Azure OpenAI / AI Foundry** (GPT-4o) | `azure-ai-openai` via `azure-ai-projects` | FNOL extraction, policy verification, entity matching, routing decisions | `claims.azure.ai.enabled=true` |
| **Azure OpenAI** (GPT-4o Vision) | `azure-ai-openai` | Image forensics narrative consistency check | `claims.azure.ai.enabled=true` |
| **Azure Computer Vision** | REST (`2024-05-01-preview`) | Image manipulation detection, metadata analysis | `claims.azure.ai.enabled=true` |
| **Azure AI Search** | REST (`2024-06-01`) | Semantic similarity search for similar claims | `claims.azure.ai.search-endpoint` set |
| **Azure Document Intelligence** | `azure-ai-documentintelligence` v1.0.7 | OCR + layout extraction from PDFs, images, Office docs | `claims.document-intelligence.enabled=true` |

All services support both API-key authentication and passwordless Entra ID (`DefaultAzureCredential`).

---

## 9. Configuration Reference

All AI configuration lives under the `claims:` Spring Boot properties block.

### AI Processing

```yaml
claims:
  processing:
    default-mode: AI_ASSISTED      # AI_ASSISTED | SEMI_AUTOMATIC | FULL_MANUAL
    stp-confidence-threshold: 85   # auto-approve above this (0–100)
    hitl-confidence-threshold: 60  # route to HITL below this (0–100)
    ai-enabled: true               # false → StubAIOrchestrationService (FULL_MANUAL)
    circuit-breaker:
      failure-threshold: 3         # failures before circuit opens
      reset-minutes: 2             # minutes before circuit attempts HALF_OPEN
```

### Azure AI Foundry (GPT-4o)

```yaml
claims:
  azure:
    ai:
      enabled: false                              # true → RealAzureAIClient active
      project-endpoint: ${AZURE_AI_PROJECT_ENDPOINT:}
        # Format: https://<resource>.services.ai.azure.com/api/projects/<project>
      api-key: ${AZURE_AI_API_KEY:}               # omit for DefaultAzureCredential
      model-deployment: ${AZURE_OPENAI_DEPLOYMENT:gpt-4o}
      search:
        endpoint: ${AZURE_SEARCH_ENDPOINT:}
        api-key: ${AZURE_SEARCH_API_KEY:}
        index: ${AZURE_SEARCH_INDEX:claims}
      vision:
        endpoint: ${AZURE_VISION_ENDPOINT:}
        api-key: ${AZURE_VISION_API_KEY:}
        api-version: ${AZURE_VISION_API_VERSION:2024-05-01-preview}
      max-retries: 3
      timeout-seconds: 30
```

### Azure Document Intelligence

```yaml
claims:
  document-intelligence:
    enabled: false                               # true → DocumentExtractionService active
    endpoint: ${DOCUMENT_INTELLIGENCE_ENDPOINT:}
    api-key: ${DOCUMENT_INTELLIGENCE_API_KEY:}
    model-id: ${DOCUMENT_INTELLIGENCE_MODEL_ID:prebuilt-layout}
```

### Environment Variables (`.env.example`)

```
AZURE_AI_PROJECT_ENDPOINT=https://<resource>.services.ai.azure.com/api/projects/<project>
AZURE_AI_API_KEY=<key-or-omit-for-managed-identity>
AZURE_OPENAI_DEPLOYMENT=gpt-4o
AZURE_SEARCH_ENDPOINT=https://<name>.search.windows.net
AZURE_SEARCH_API_KEY=<key>
AZURE_SEARCH_INDEX=claims
AZURE_VISION_ENDPOINT=https://<name>.cognitiveservices.azure.com
AZURE_VISION_API_KEY=<key>
DOCUMENT_INTELLIGENCE_ENDPOINT=https://<name>.cognitiveservices.azure.com
DOCUMENT_INTELLIGENCE_API_KEY=<key>
```

---

## 10. Frontend AI Screens

The React SPA (`claims-frontend`) exposes four screens that surface AI pipeline outputs to adjusters and operations staff.

### Forensics Screen (`src/screens/Forensics.tsx`)

Displays the output of Step 5 (image forensics):

- **Image Analysis panel** (70 % width) — renders the evidence image with overlay badges showing manipulation warnings.
- **Authenticity Score** — numeric score from the `ForensicsResult.overallScore` (0–100 %); colour-coded red for high risk.
- **Narrative Consistency** — side-by-side comparison of the extracted incident text vs. the GPT-4o AI classification of image content.
- **Analysis Options** — toggles for heatmap overlay, edge detection, metadata verification.
- **Route to SIU** button — adjuster action to escalate to Special Investigations Unit.

### Duplicates Screen (`src/screens/Duplicates.tsx`)

Displays the output of Step 6 (duplicate detection):

- **Current FNOL card** — shows claim ID, vessel, date of loss, location, description, and estimated value.
- **Historical Claims table** — lists candidate duplicates with similarity percentage (from `DuplicateMatch.similarityScore`). Rows with similarity ≥ 90 % are highlighted amber.
- **Actions** — "Link as Subsequent Report" or "Dismiss" per candidate.

### Entities Screen (`src/screens/Entities.tsx`)

Displays the output of Step 4 (entity matching):

- **Extracted Entities panel** — raw names pulled from the FNOL (assured, broker, vessel).
- **Proposed Matches panel** — AI-matched names from the registry with confidence scores; adjuster can "Accept Match".
- **Match Confidence panel** — colour-coded confidence badges (green ≥ 90 %, amber 80–89 %).

### HITL Console (`src/screens/HITLConsole.tsx`)

The adjuster's review workspace shown when a claim is routed to Human-in-the-Loop:

- **Processing Pipeline stepper** — visual progress indicator for all 7 pipeline steps, highlighting the current step.
- **Adjuster Review form** — free-text notes, reserve amount, deductible, and payout recommendation fields.
- **Actions** — "Request More Information" or "Approve & Commit to Core" (triggers core system sync).

---

## Summary

The AI pipeline in Horus Claims is a layered, resilient system:

- **Azure AI Foundry GPT-4o** drives all natural-language tasks: FNOL extraction, policy verification, entity matching, image narrative consistency, and ReAct-based routing decisions.
- **Azure Document Intelligence** provides OCR and structured layout extraction as the document ingestion pre-step.
- **Azure AI Search / PostgreSQL FTS** provides semantic similarity search for similar past claims.
- **Azure Computer Vision** provides low-level image manipulation detection in the forensics pass.
- **Local TF-IDF** (`TextSimilarityDuplicateDetector`) ensures duplicate detection always works without external credentials.
- **Circuit breaker + retry + mock/stub fallbacks** ensure claims are never stuck — they degrade to human review rather than failing.
- **Caffeine LRU cache** and **Micrometer metrics** provide cost control and observability.
