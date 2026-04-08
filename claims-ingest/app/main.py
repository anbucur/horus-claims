import os
import json
from datetime import datetime
from typing import Optional, List, Dict, Any
from contextlib import asynccontextmanager

import psycopg2
from psycopg2.extras import RealDictCursor
import redis
from kafka import KafkaConsumer, KafkaProducer
from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://claims_user:claims_secure_pass@postgres:5432/claims"
)
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
VALKEY_HOST = os.getenv("VALKEY_HOST", "valkey")
VALKEY_PASSWORD = os.getenv("VALKEY_PASSWORD", "valkey_secure_pass")

INGEST_TOPIC = "claims-fnohl-ingest"
VALIDATION_TOPIC = "claims-validation"
AUDIT_TOPIC = "claims-audit"

valkey = None

def get_redis():
    global valkey
    if valkey is None:
        valkey = redis.Redis(host=VALKEY_HOST, port=6379,
                              password=VALKEY_PASSWORD, decode_responses=True)
    return valkey

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

@asynccontextmanager
async def lifespan(app: FastAPI):
    consumer = KafkaConsumer(
        INGEST_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        auto_offset_reset="earliest",
        enable_auto_commit=True,
        group_id="claims-ingest-service"
    )
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8")
    )
    yield
    consumer.close()
    producer.close()

app = FastAPI(title="Claims Ingest Service", lifespan=lifespan)

class IngestPayload(BaseModel):
    claim_id: Optional[str] = None
    source: str
    payload: Dict[str, Any]
    received_at: Optional[str] = None

class ValidationResult(BaseModel):
    claim_id: str
    is_valid: bool
    errors: List[str] = []
    warnings: List[str] = []

def validate_claim(payload: Dict[str, Any]) -> ValidationResult:
    errors = []
    warnings = []
    required_fields = ["date_of_loss", "loss_location", "insured_name"]
    for field in required_fields:
        if field not in payload or not payload[field]:
            errors.append(f"Missing required field: {field}")
    if "policy_number" not in payload:
        warnings.append("No policy number provided — policy verification required")
    return ValidationResult(
        claim_id=payload.get("claim_id", "unknown"),
        is_valid=len(errors) == 0,
        errors=errors,
        warnings=warnings
    )

@app.post("/ingest")
async def ingest_claim(item: IngestPayload):
    received_at = item.received_at or datetime.utcnow().isoformat()
    r = get_redis()
    cache_key = f"ingest:{item.claim_id or item.source}:{received_at}"
    r.setex(cache_key, 3600, json.dumps(item.model_dump()))
    return {"status": "accepted", "cache_key": cache_key}

@app.get("/health")
async def health():
    try:
        get_db().close()
        get_redis().ping()
        return {"status": "healthy", "postgres": "up", "redis": "up"}
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))

@app.get("/claims/{claim_id}/status")
async def claim_status(claim_id: str):
    r = get_redis()
    keys = r.keys(f"ingest:{claim_id}:*")
    if not keys:
        raise HTTPException(status_code=404, detail="Claim not found")
    return {"claim_id": claim_id, "events": [json.loads(r.get(k)) for k in keys]}
