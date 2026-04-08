package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Result of LLM schema-based extraction from raw document text.
 * Each extracted field carries its own confidence score.
 */
@Data
@Builder
public class DocumentExtractionResult {

    /** Whether the LLM successfully extracted data from the document text. */
    private boolean extracted;

    /** The document ID or filename the extraction was performed on. */
    private String sourceDocumentId;

    /** The model used for extraction (e.g. gpt-4o). */
    private String modelUsed;

    /** Per-field extraction results keyed by schema field name. */
    private Map<String, ExtractedField> fields;

    /** Overall confidence score for the entire document (average of field scores). */
    private double overallConfidenceScore;

    /** The raw text that was sent to the LLM for extraction. */
    private String rawExtractedText;

    /** Error message if extraction failed. */
    private String errorMessage;

    /**
     * A single extracted field with its value and confidence.
     */
    @Data
    @Builder
    public static class ExtractedField {
        /** The extracted value as a string. */
        private String value;

        /** Confidence score for this specific field (0.0–1.0). */
        private double confidenceScore;

        /** The source text span from which this field was extracted (for traceability). */
        private String sourceTextSpan;

        /** Error or warning for this specific field (e.g. "empty", "ambiguous"). */
        private String warning;
    }
}
