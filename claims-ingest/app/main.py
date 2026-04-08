import os
import json
import logging
import threading
from datetime import datetime
from typing import Optional, List, Dict, Any
from contextlib import asynccontextmanager
from enum import Enum

import psycopg2
from psycopg2.extras import RealDictCursor
import redis
from kafka import KafkaConsumer, KafkaProducer
from fastapi import FastAPI, HTTPException, BackgroundTasks, Header, Request
from pydantic import BaseModel, EmailStr, Field

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("claims-ingest")

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
kafka_producer = None
kafka_consumer_thread = None


def get_redis():
    global valkey
    if valkey is None:
        valkey = redis.Redis(
            host=VALKEY_HOST,
            port=6379,
            password=VALKEY_PASSWORD,
            decode_responses=True
        )
    return valkey


def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)


class EmailAttachment(BaseModel):
    filename: str
    content_type: str
    url: str


class EmailPayload(BaseModel):
    message_id: str
    from_address: EmailStr
    to_addresses: List[EmailStr]
    subject: str
    body: str
    received_at: str
    attachments: List[EmailAttachment] = []
    claim_id: Optional[str] = None


class ClaimEvent(BaseModel):
    event_id: str
    claim_id: str
    event_type: str
    source: str
    payload: Dict[str, Any]
    timestamp: str
    correlation_id: Optional[str] = None


class ValidationResult(BaseModel):
    claim_id: str
    is_valid: bool
    errors: List[str] = []
    warnings: List[str] = []


class EventType(str, Enum):
    EMAIL_RECEIVED = "EMAIL_RECEIVED"
    CLAIM_CREATED = "CLAIM_CREATED"
    CLAIM_VALIDATED = "CLAIM_VALIDATED"
    CLAIM_EXTRACTED = "CLAIM_EXTRACTED"
    CLAIM_VERIFIED = "CLAIM_VERIFIED"
    CLAIM_ROUTED_HITL = "CLAIM_ROUTED_HITL"
    CLAIM_APPROVED_STP = "CLAIM_APPROVED_STP"


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


def publish_to_kafka(topic: str, message: Dict[str, Any]):
    global kafka_producer
    if kafka_producer is None:
        logger.warning("Kafka producer not initialized, skipping publish to %s", topic)
        return
    try:
        kafka_producer.send(topic, value=message)
        kafka_producer.flush()
        logger.info("Published message to topic %s: claim_id=%s", topic, message.get("claim_id"))
    except Exception as e:
        logger.error("Failed to publish to Kafka topic %s: %s", topic, str(e))


def store_claim_event(claim_id: str, event: ClaimEvent):
    r = get_redis()
    event_key = f"events:{claim_id}:{event.event_id}"
    r.setex(event_key, 86400, json.dumps(event.model_dump()))
    r.zadd(f"events:{claim_id}", {event_key: event.timestamp})
    logger.info("Stored event for claim %s: type=%s", claim_id, event.event_type)


def kafka_validation_consumer():
    global kafka_producer
    logger.info("Starting Kafka validation consumer thread")
    try:
        consumer = KafkaConsumer(
            VALIDATION_TOPIC,
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            auto_offset_reset="earliest",
            enable_auto_commit=True,
            group_id="claims-ingest-validation-consumer",
            value_deserializer=lambda m: json.loads(m.decode("utf-8"))
        )
        logger.info("Kafka validation consumer connected, listening on %s", VALIDATION_TOPIC)
        for message in consumer:
            try:
                data = message.value
                logger.info("Received validation result: claim_id=%s, is_valid=%s",
                          data.get("claim_id"), data.get("is_valid"))
            except Exception as e:
                logger.error("Error processing validation message: %s", str(e))
    except Exception as e:
        logger.error("Kafka validation consumer error: %s", str(e))


@asynccontextmanager
async def lifespan(app: FastAPI):
    global kafka_producer, kafka_consumer_thread
    
    logger.info("Initializing Claims Ingest Service")
    
    kafka_producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8") if k else None
    )
    logger.info("Kafka producer initialized")
    
    kafka_consumer_thread = threading.Thread(
        target=kafka_validation_consumer,
        daemon=True
    )
    kafka_consumer_thread.start()
    logger.info("Kafka validation consumer thread started")
    
    yield
    
    logger.info("Shutting down Claims Ingest Service")
    if kafka_producer:
        kafka_producer.close()
    logger.info("Shutdown complete")


app = FastAPI(
    title="Claims Ingest Service",
    description="Ingestion service for MSIG Claims Platform",
    version="1.0.0",
    lifespan=lifespan
)


@app.post("/ingest")
async def ingest_claim(item: IngestPayload):
    received_at = item.received_at or datetime.utcnow().isoformat()
    r = get_redis()
    cache_key = f"ingest:{item.claim_id or item.source}:{received_at}"
    r.setex(cache_key, 3600, json.dumps(item.model_dump()))
    
    event = ClaimEvent(
        event_id=f"{item.claim_id or item.source}-{received_at}",
        claim_id=item.claim_id or "unknown",
        event_type=EventType.CLAIM_CREATED.value,
        source=item.source,
        payload=item.payload,
        timestamp=received_at
    )
    store_claim_event(event.claim_id, event)
    
    publish_to_kafka(INGEST_TOPIC, {
        "claim_id": item.claim_id,
        "source": item.source,
        "payload": item.payload,
        "received_at": received_at
    })
    
    logger.info("Claim ingested: claim_id=%s, source=%s", item.claim_id, item.source)
    return {"status": "accepted", "claim_id": item.claim_id}


@app.post("/email/webhook")
async def email_webhook(
    payload: EmailPayload,
    x_webhook_secret: Optional[str] = Header(None)
):
    logger.info("Email webhook received: message_id=%s, from=%s",
                payload.message_id, payload.from_address)
    
    if not payload.claim_id:
        logger.warning("Email webhook received without claim_id: message_id=%s", payload.message_id)
        raise HTTPException(status_code=400, detail="claim_id is required in email payload")
    
    event = ClaimEvent(
        event_id=payload.message_id,
        claim_id=payload.claim_id,
        event_type=EventType.EMAIL_RECEIVED.value,
        source="email",
        payload={
            "message_id": payload.message_id,
            "from": payload.from_address,
            "to": payload.to_addresses,
            "subject": payload.subject,
            "body": payload.body,
            "attachments": [a.model_dump() for a in payload.attachments]
        },
        timestamp=payload.received_at,
        correlation_id=payload.message_id
    )
    store_claim_event(payload.claim_id, event)
    
    publish_to_kafka(INGEST_TOPIC, {
        "claim_id": payload.claim_id,
        "source": "email",
        "payload": event.payload,
        "received_at": payload.received_at
    })
    
    logger.info("Email processed and published: claim_id=%s, message_id=%s",
                payload.claim_id, payload.message_id)
    return {"status": "processed", "claim_id": payload.claim_id, "event_id": payload.message_id}


@app.get("/claims/{claim_id}/events")
async def get_claim_events(claim_id: str, limit: int = 100):
    logger.info("Retrieving events for claim: %s", claim_id)
    r = get_redis()
    event_keys = r.zrevrange(f"events:{claim_id}", 0, limit - 1)
    
    if not event_keys:
        raise HTTPException(status_code=404, detail=f"No events found for claim {claim_id}")
    
    events = []
    for key in event_keys:
        event_data = r.get(key)
        if event_data:
            events.append(json.loads(event_data))
    
    logger.info("Retrieved %d events for claim %s", len(events), claim_id)
    return {
        "claim_id": claim_id,
        "count": len(events),
        "events": events
    }


@app.post("/claims/{claim_id}/validate")
async def validate_claim_endpoint(claim_id: str):
    logger.info("Validating claim: %s", claim_id)
    r = get_redis()
    
    keys = r.keys(f"ingest:{claim_id}:*")
    if not keys:
        raise HTTPException(status_code=404, detail=f"No claim data found for {claim_id}")
    
    latest_key = sorted(keys)[-1]
    claim_data = json.loads(r.get(latest_key))
    payload = claim_data.get("payload", {})
    
    result = validate_claim(payload)
    
    event = ClaimEvent(
        event_id=f"{claim_id}-validation-{datetime.utcnow().isoformat()}",
        claim_id=claim_id,
        event_type=EventType.CLAIM_VALIDATED.value,
        source="validation-service",
        payload=result.model_dump(),
        timestamp=datetime.utcnow().isoformat()
    )
    store_claim_event(claim_id, event)
    
    publish_to_kafka(VALIDATION_TOPIC, {
        "claim_id": claim_id,
        "is_valid": result.is_valid,
        "errors": result.errors,
        "warnings": result.warnings,
        "validated_at": datetime.utcnow().isoformat()
    })
    
    logger.info("Claim validation complete: claim_id=%s, is_valid=%s", claim_id, result.is_valid)
    return result


@app.get("/health")
async def health():
    try:
        get_db().close()
        get_redis().ping()
        return {
            "status": "healthy",
            "postgres": "up",
            "redis": "up",
            "kafka_producer": "initialized" if kafka_producer else "not_initialized"
        }
    except Exception as e:
        logger.error("Health check failed: %s", str(e))
        raise HTTPException(status_code=503, detail=str(e))


@app.get("/claims/{claim_id}/status")
async def claim_status(claim_id: str):
    r = get_redis()
    keys = r.keys(f"ingest:{claim_id}:*")
    if not keys:
        raise HTTPException(status_code=404, detail="Claim not found")
    return {"claim_id": claim_id, "events": [json.loads(r.get(k)) for k in keys]}


class IngestPayload(BaseModel):
    claim_id: Optional[str] = None
    source: str
    payload: Dict[str, Any]
    received_at: Optional[str] = None


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)