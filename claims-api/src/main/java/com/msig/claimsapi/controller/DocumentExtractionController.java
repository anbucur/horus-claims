package com.msig.claimsapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.repository.EvidenceRepository;
import com.msig.claimsapi.service.DocumentExtractionService;
import com.msig.claimsapi.service.LLMSchemaExtractor;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.model.DocumentExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST API for Azure Document Intelligence + LLM extraction pipeline.
 *
 * Pipeline per document:
 * 1. [Azure DI]  OCR + layout analysis → raw text
 * 2. [GPT-4o]    Schema extraction → structured JSON with per-field confidence scores
 *
 * Usage:
 * - POST /api/documents/extract        : upload file + schema JSON → extract
 * - POST /api/documents/extract-url     : extract from URL + schema JSON
 * - POST /api/documents/extract-claim  : extract all evidence for a claim with given schema
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
public class DocumentExtractionController {

    private final DocumentExtractionService documentService;
    private final LLMSchemaExtractor llmExtractor;
    private final EvidenceRepository evidenceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Upload a single document and extract data using the provided schema.
     *
     * @param file   The document file (PDF, image, DOCX, XLSX, etc.)
     * @param schema JSON string of the extraction schema (field definitions)
     * @return DocumentExtractionResult with per-field confidence scores
     */
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentExtractionResult> extractFromUpload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("schema") String schema) {

        log.info("POST /api/documents/extract — file: {}, size: {} bytes",
            file.getOriginalFilename(), file.getSize());

        LLMSchemaExtractor.ExtractionSchema extractionSchema;
        try {
            extractionSchema = objectMapper.readValue(schema, LLMSchemaExtractor.ExtractionSchema.class);
        } catch (JsonProcessingException e) {
            log.error("Invalid schema JSON: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        try {
            DocumentExtractionResult result = llmExtractor.extract(
                file.getBytes(),
                file.getOriginalFilename(),
                extractionSchema
            );
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Extraction failed for {}: {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DocumentExtractionResult.builder()
                    .extracted(false)
                    .sourceDocumentId(file.getOriginalFilename())
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    /**
     * Extract from a publicly accessible URL (SAS, HTTP, etc.).
     *
     * @param documentUrl URL of the document
     * @param fileName   Display name for the document
     * @param schema     JSON extraction schema
     * @return DocumentExtractionResult
     */
    @PostMapping("/extract-url")
    public ResponseEntity<DocumentExtractionResult> extractFromUrl(
            @RequestParam String documentUrl,
            @RequestParam String fileName,
            @RequestParam String schema) {

        log.info("POST /api/documents/extract-url — url: {}, file: {}", documentUrl, fileName);

        LLMSchemaExtractor.ExtractionSchema extractionSchema;
        try {
            extractionSchema = objectMapper.readValue(schema, LLMSchemaExtractor.ExtractionSchema.class);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            DocumentExtractionResult result = llmExtractor.extractFromUrl(
                documentUrl, fileName, extractionSchema
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("URL extraction failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DocumentExtractionResult.builder()
                    .extracted(false)
                    .sourceDocumentId(fileName)
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    /**
     * Extract all evidence documents attached to a claim using the provided schema.
     * Stores results back on each Evidence entity.
     *
     * @param claimId The claim ID
     * @param schema  JSON extraction schema (shared across all documents)
     * @return Summary of extraction results per document
     */
    @PostMapping("/extract-claim/{claimId}")
    public ResponseEntity<List<DocumentExtractionResult>> extractClaimDocuments(
            @PathVariable Long claimId,
            @RequestParam String schema) {

        log.info("POST /api/documents/extract-claim/{} — schema fields: ...", claimId);

        LLMSchemaExtractor.ExtractionSchema extractionSchema;
        try {
            extractionSchema = objectMapper.readValue(schema, LLMSchemaExtractor.ExtractionSchema.class);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }

        List<Evidence> evidences = evidenceRepository.findByClaimId(claimId);
        if (evidences.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<DocumentExtractionResult> results = new java.util.ArrayList<>();

        for (Evidence evidence : evidences) {
            if (evidence.getFileUrl() == null || evidence.getFileUrl().isBlank()) {
                log.warn("Evidence {} has no fileUrl, skipping", evidence.getId());
                continue;
            }

            try {
                DocumentExtractionResult result = llmExtractor.extractFromUrl(
                    evidence.getFileUrl(),
                    evidence.getFileName() != null ? evidence.getFileName() : "evidence-" + evidence.getId(),
                    extractionSchema
                );

                // Persist results on the Evidence entity
                evidence.setAiExtractedText(result.getRawExtractedText());
                try {
                    evidence.setAiExtractionSchema(objectMapper.writeValueAsString(result.getFields()));
                } catch (JsonProcessingException e) {
                    log.warn("Could not serialize extraction fields for evidence {}: {}",
                        evidence.getId(), e.getMessage());
                }
                evidence.setAiExtractionConfidenceScore(result.getOverallConfidenceScore());
                evidenceRepository.save(evidence);

                results.add(result);

            } catch (Exception e) {
                log.error("Extraction failed for evidence {}: {}", evidence.getId(), e.getMessage());
                results.add(DocumentExtractionResult.builder()
                    .extracted(false)
                    .sourceDocumentId(evidence.getFileName())
                    .errorMessage(e.getMessage())
                    .build());
            }
        }

        log.info("Extracted {} documents for claim {}", results.size(), claimId);
        return ResponseEntity.ok(results);
    }

    /**
     * Preview DI OCR output for a document without LLM extraction.
     * Useful for validating what DI extracts before designing the schema.
     */
    @PostMapping("/preview")
    public ResponseEntity<String> previewDocument(@RequestParam String documentUrl) {
        log.info("POST /api/documents/preview — url: {}", documentUrl);
        try {
            DocumentExtractionService.RawDocumentContent raw =
                documentService.analyseDocumentFromUrl(documentUrl, documentUrl);
            String text = documentService.toLLMInputText(raw);
            return ResponseEntity.ok(text);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Preview failed: " + e.getMessage());
        }
    }
}
