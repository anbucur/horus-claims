package com.msig.claimsapi.service;

import com.msig.claimsapi.config.AzureAIConfig;
import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.ClaimSimilarityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final ClaimRepository claimRepository;
    private final AzureAIConfig azureAIConfig;
    private final RestTemplate restTemplate;

    /**
     * Find similar past claims based on narrative, loss type, and location.
     * Uses Azure AI Search if configured; falls back to PostgreSQL full-text search.
     */
    public List<ClaimSimilarityResult> findSimilarClaims(Claim claim, int limit) {
        String queryText = buildQueryText(claim);

        if (isAzureSearchConfigured()) {
            try {
                return searchWithAzureAI(queryText, claim.getId(), limit);
            } catch (Exception e) {
                log.warn("Azure AI Search failed, falling back to keyword search: {}", e.getMessage());
                return searchWithKeywords(queryText, claim.getId(), limit);
            }
        }

        return searchWithKeywords(queryText, claim.getId(), limit);
    }

    private String buildQueryText(Claim claim) {
        StringBuilder sb = new StringBuilder();
        if (claim.getIncidentNarrative() != null) sb.append(claim.getIncidentNarrative()).append(" ");
        if (claim.getLossLocation() != null) sb.append(claim.getLossLocation()).append(" ");
        return sb.toString().trim();
    }

    private boolean isAzureSearchConfigured() {
        return azureAIConfig.isEnabled()
            && azureAIConfig.getSearchEndpoint() != null
            && !azureAIConfig.getSearchEndpoint().isEmpty()
            && azureAIConfig.getSearchApiKey() != null
            && !azureAIConfig.getSearchApiKey().isEmpty();
    }

    private List<ClaimSimilarityResult> searchWithAzureAI(String query, Long excludeClaimId, int limit) {
        String url = azureAIConfig.getSearchEndpoint()
            + "/indexes/" + azureAIConfig.getSearchIndex()
            + "/docs/search?api-version=2024-06-01";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", azureAIConfig.getSearchApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("searchText", query);
        requestBody.put("top", limit);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        var response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        List<ClaimSimilarityResult> results = new ArrayList<>();
        if (response.getBody() != null && response.getBody().get("value") != null) {
            List<Map<String, Object>> hits = (List<Map<String, Object>>) response.getBody().get("value");
            for (Map<String, Object> hit : hits) {
                Object claimIdObj = hit.get("claimId");
                if (claimIdObj == null) continue;
                Long claimId = claimIdObj instanceof Number
                    ? ((Number) claimIdObj).longValue()
                    : Long.parseLong(claimIdObj.toString());
                if (claimId.equals(excludeClaimId)) continue;

                Double score = hit.get("@search.score") != null
                    ? ((Number) hit.get("@search.score")).doubleValue()
                    : 0.0;

                results.add(new ClaimSimilarityResult(
                    claimId,
                    (String) hit.get("claimReference"),
                    (String) hit.get("incidentNarrative"),
                    score,
                    "Azure AI Search similarity"
                ));
            }
        }
        return results;
    }

    private List<ClaimSimilarityResult> searchWithKeywords(String query, Long excludeClaimId, int limit) {
        try {
            List<Object[]> rows = claimRepository.findSimilarByText(query, excludeClaimId, limit);
            List<ClaimSimilarityResult> results = new ArrayList<>();
            for (Object[] row : rows) {
                Long claimId = ((Number) row[0]).longValue();
                results.add(new ClaimSimilarityResult(
                    claimId,
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).doubleValue(),
                    "PostgreSQL full-text match"
                ));
            }
            return results;
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return List.of();
        }
    }
}
