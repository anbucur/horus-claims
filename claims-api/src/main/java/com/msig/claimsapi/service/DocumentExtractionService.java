package com.msig.claimsapi.service;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeOperationDetails;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzedDocument;
import com.azure.ai.documentintelligence.models.DocumentKeyValueElement;
import com.azure.ai.documentintelligence.models.DocumentKeyValuePair;
import com.azure.ai.documentintelligence.models.DocumentLanguage;
import com.azure.ai.documentintelligence.models.DocumentLine;
import com.azure.ai.documentintelligence.models.DocumentPage;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.ai.documentintelligence.models.DocumentTableCell;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.DocumentIntelligenceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Document extraction via Azure Document Intelligence SDK v1.0.7 (API 2024-11-30).
 *
 * Pipeline:
 * 1. [Azure DI SDK] Analyze document (OCR + layout) → AnalyzeResult with pages, tables, key-value pairs
 * 2. [GPT-4o]      Send structured text to LLM       → structured JSON with per-field confidence scores
 *
 * Uses the official azure-ai-documentintelligence library, NOT the deprecated
 * azure-ai-formrecognizer (Form Recognizer) or raw REST calls.
 *
 * Supported input: PDF, image (PNG/JPG/TIFF/BMP/HEIC), DOCX, XLSX, PPTX.
 *
 * Default model: prebuilt-layout (general layout/OCR).
 * The retired prebuilt-document model is replaced by prebuilt-layout with optional
 * DocumentAnalysisFeature.FORMULAS for formula extraction.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "claims.document-intelligence.enabled", havingValue = "true")
public class DocumentExtractionService {

    private final DocumentIntelligenceConfig config;
    private final DocumentIntelligenceClient client;
    private final ObjectMapper objectMapper;

    public DocumentExtractionService(DocumentIntelligenceConfig config,
                                     DocumentIntelligenceClient documentIntelligenceClient,
                                     ObjectMapper objectMapper) {
        this.config = config;
        this.client = documentIntelligenceClient;
        this.objectMapper = objectMapper;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Analyse a document from a byte array.
     *
     * @param documentBytes raw file bytes (PDF, image, Office)
     * @param fileName      display name for the document
     * @return RawDocumentContent with all extracted text, tables, key-value pairs, entities
     */
    public RawDocumentContent analyseDocument(byte[] documentBytes, String fileName) {
        log.info("[DocIntel] Analysing document ({} bytes): {}", documentBytes.length, fileName);

        try {
            BinaryData binaryData = BinaryData.fromBytes(documentBytes);
            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(binaryData);

            SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller =
                client.beginAnalyzeDocument(config.getModelId(), options);

            AnalyzeResult result = poller.getFinalResult();

            return parseAnalyzeResult(result, fileName);

        } catch (Exception e) {
            log.error("[DocIntel] Document analysis failed for {}: {}", fileName, e.getMessage(), e);
            return RawDocumentContent.failed(fileName, e.getMessage());
        }
    }

    /**
     * Analyse a document from a publicly accessible URL.
     */
    public RawDocumentContent analyseDocumentFromUrl(String documentUrl, String fileName) {
        log.info("[DocIntel] Analysing document from URL: {}", documentUrl);

        try {
            AnalyzeDocumentOptions options = new AnalyzeDocumentOptions(documentUrl);

            SyncPoller<AnalyzeOperationDetails, AnalyzeResult> poller =
                client.beginAnalyzeDocument(config.getModelId(), options);

            AnalyzeResult result = poller.getFinalResult();
            return parseAnalyzeResult(result, fileName);

        } catch (Exception e) {
            log.error("[DocIntel] Document URL analysis failed for {}: {}", documentUrl, e.getMessage(), e);
            return RawDocumentContent.failed(fileName, e.getMessage());
        }
    }

    /**
     * Convert the RawDocumentContent to a Markdown-like text format
     * suitable for ingestion by GPT-4o.
     */
    public String toLLMInputText(RawDocumentContent doc) {
        if (!doc.successful()) {
            return "[Document analysis failed: " + doc.errorMessage() + "]";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("## Document Metadata\n");
        sb.append("File: ").append(doc.fileName()).append("\n");
        sb.append("Languages: ").append(String.join(", ", doc.languages())).append("\n");
        sb.append("Page count: ").append(doc.pageCount()).append("\n\n");

        if (!doc.textLines().isEmpty()) {
            sb.append("## Document Text\n");
            for (TextLine line : doc.textLines()) {
                sb.append(line.lineNumber())
                  .append(". ")
                  .append(line.text());
                if (!line.labels().isEmpty()) {
                    sb.append(" [").append(String.join(", ", line.labels())).append("]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!doc.tables().isEmpty()) {
            sb.append("## Tables\n");
            int n = 1;
            for (TableContent table : doc.tables()) {
                sb.append("### Table ").append(n++).append(" (rows=")
                  .append(table.rowCount()).append(", cols=").append(table.columnCount()).append(")\n");
                for (List<String> row : table.rows()) {
                    sb.append("| ").append(String.join(" | ", row)).append(" |\n");
                }
                sb.append("\n");
            }
        }

        if (!doc.keyValuePairs().isEmpty()) {
            sb.append("## Key-Value Pairs\n");
            for (Map.Entry<String, String> kv : doc.keyValuePairs().entrySet()) {
                sb.append("- **").append(kv.getKey()).append("**: ");
                sb.append(kv.getValue() != null ? kv.getValue() : "[not detected]");
                sb.append("\n");
            }
            sb.append("\n");
        }

        if (!doc.entities().isEmpty()) {
            sb.append("## Detected Entities\n");
            for (Map.Entry<String, List<String>> e : doc.entities().entrySet()) {
                sb.append("- **").append(e.getKey()).append("**: ")
                  .append(String.join(", ", e.getValue())).append("\n");
            }
        }

        return sb.toString();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private RawDocumentContent parseAnalyzeResult(AnalyzeResult result, String fileName) {
        if (result == null) {
            return RawDocumentContent.failed(fileName, "Null result from Document Intelligence");
        }

        List<TextLine> textLines = new ArrayList<>();
        List<TableContent> tables = new ArrayList<>();
        Map<String, String> kvPairs = new LinkedHashMap<>();
        Map<String, List<String>> entities = new LinkedHashMap<>();
        List<String> languages = new ArrayList<>();
        int pageCount = 0;

        // Languages (top-level in AnalyzeResult)
        if (result.getLanguages() != null) {
            for (DocumentLanguage lang : result.getLanguages()) {
                String locale = lang.getLocale();
                if (locale != null && !locale.isBlank() && !languages.contains(locale)) {
                    languages.add(locale);
                }
            }
        }

        // Pages
        if (result.getPages() != null) {
            pageCount = result.getPages().size();
            for (DocumentPage page : result.getPages()) {
                // Text lines
                if (page.getLines() != null) {
                    for (DocumentLine line : page.getLines()) {
                        String text = line.getContent();
                        List<String> labels = detectLineLabels(text);
                        textLines.add(new TextLine(textLines.size() + 1, text, labels));
                    }
                }
            }
        }

        // Tables
        if (result.getTables() != null) {
            for (DocumentTable table : result.getTables()) {
                int rowCount = table.getRowCount();
                int colCount = table.getColumnCount();
                List<List<String>> rows = new ArrayList<>();

                if (table.getCells() != null) {
                    // Group cells by row index
                    Map<Integer, List<String>> rowMap = new LinkedHashMap<>();
                    for (DocumentTableCell cell : table.getCells()) {
                        int rowIndex = cell.getRowIndex();
                        String cellContent = cell.getContent();
                        rowMap.computeIfAbsent(rowIndex, k -> new ArrayList<>()).add(cellContent);
                    }
                    // Emit rows in order, filling empty slots
                    for (int r = 0; r < rowCount; r++) {
                        List<String> row = rowMap.get(r);
                        if (row != null) {
                            rows.add(row);
                        } else {
                            rows.add(Collections.nCopies(colCount, ""));
                        }
                    }
                }
                tables.add(new TableContent(rowCount, colCount, rows));
            }
        }

        // Key-value pairs
        if (result.getKeyValuePairs() != null) {
            for (DocumentKeyValuePair kvp : result.getKeyValuePairs()) {
                String key = kvp.getKey() != null ? kvp.getKey().getContent() : null;
                String value = kvp.getValue() != null ? kvp.getValue().getContent() : null;
                if (key != null && !key.isBlank()) {
                    kvPairs.put(key.trim(), value);
                }
            }
        }

        // Document entities (from the "documents" array — prebuilt-layout model output)
        if (result.getDocuments() != null) {
            for (AnalyzedDocument docNode : result.getDocuments()) {
                if (docNode.getFields() != null) {
                    docNode.getFields().forEach((fieldName, fieldValue) -> {
                        String content = fieldValue != null ? fieldValue.getContent() : null;
                        if (content != null && !content.isBlank()) {
                            entities.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(content);
                        }
                    });
                }
            }
        }

        return RawDocumentContent.success(fileName, textLines, tables, kvPairs, entities, languages, pageCount);
    }

    private List<String> detectLineLabels(String text) {
        List<String> labels = new ArrayList<>();
        if (text == null || text.isBlank()) return labels;
        if (text.endsWith(":")) {
            labels.add("field-label");
        }
        String[] words = text.trim().split("\\s+");
        if (words.length > 0 && words.length <= 6 && text.equals(text.toUpperCase()) && !text.toLowerCase().equals(text)) {
            labels.add("heading");
        }
        return labels;
    }

    // ─── Inner data classes ───────────────────────────────────────────────────

    public record RawDocumentContent(
        String fileName,
        boolean successful,
        String errorMessage,
        List<TextLine> textLines,
        List<TableContent> tables,
        Map<String, String> keyValuePairs,
        Map<String, List<String>> entities,
        List<String> languages,
        int pageCount
    ) {
        public static RawDocumentContent success(String fileName, List<TextLine> textLines,
                List<TableContent> tables, Map<String, String> kvPairs,
                Map<String, List<String>> entities, List<String> languages, int pageCount) {
            return new RawDocumentContent(fileName, true, null, textLines, tables, kvPairs, entities, languages, pageCount);
        }
        public static RawDocumentContent failed(String fileName, String error) {
            return new RawDocumentContent(fileName, false, error, List.of(), List.of(), Map.of(), Map.of(), List.of(), 0);
        }
    }

    public record TextLine(int lineNumber, String text, List<String> labels) {}
    public record TableContent(int rowCount, int columnCount, List<List<String>> rows) {}
}
