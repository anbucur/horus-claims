package com.msig.claimsapi.service.ai;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.SubjectMatterInsured;
import com.msig.claimsdomain.model.DuplicateMatch;
import com.msig.claimsapi.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Text-similarity-based duplicate claims detection using TF-IDF cosine similarity.
 * Works locally without external AI credentials — runs on claim text fields only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TextSimilarityDuplicateDetector {

    private final ClaimRepository claimRepository;
    private static final double THRESHOLD = 0.60;

    /**
     * Finds claims semantically similar to the given claim based on text similarity.
     */
    public List<DuplicateMatch> findDuplicates(Claim claim) {
        String queryText = buildText(claim);
        List<Claim> candidates = claimRepository.findAll().stream()
                .filter(c -> !c.getId().equals(claim.getId()))
                .filter(c -> c.getWorkflowStatus() != Claim.WorkflowStatus.RECEIVED)
                .toList();

        Map<Long, Double> scores = new HashMap<>();
        for (Claim candidate : candidates) {
            String candidateText = buildText(candidate);
            double sim = cosineSimilarity(queryText, candidateText);
            if (sim >= THRESHOLD) {
                scores.put(candidate.getId(), sim);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(e -> {
                    Claim matched = candidates.stream()
                            .filter(c -> c.getId().equals(e.getKey())).findFirst().orElse(null);
                    if (matched == null) return null;
                    String vessel = getVesselName(matched);
                    String matchReason = buildMatchReason(queryText, buildText(matched));
                    return DuplicateMatch.builder()
                            .claimId(matched.getId())
                            .claimReference(matched.getPolicy() != null ? matched.getPolicy().getPolicyNumber() : null)
                            .similarityScore(e.getValue())
                            .matchReason(matchReason)
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private String buildText(Claim claim) {
        StringBuilder sb = new StringBuilder();
        if (claim.getIncidentNarrative() != null) sb.append(claim.getIncidentNarrative()).append(" ");
        if (claim.getLossLocation() != null) sb.append(claim.getLossLocation()).append(" ");
        String vessel = getVesselName(claim);
        if (vessel != null) sb.append(vessel).append(" ");
        return sb.toString().trim().toLowerCase();
    }

    private String getVesselName(Claim claim) {
        if (claim.getSubjectMattersInsured() == null) return null;
        return claim.getSubjectMattersInsured().stream()
                .filter(s -> s.getType() == SubjectMatterInsured.SubjectType.VESSEL && s.getName() != null)
                .map(SubjectMatterInsured::getName)
                .findFirst().orElse(null);
    }

    private String buildMatchReason(String textA, String textB) {
        Set<String> wordsA = new HashSet<>(Arrays.asList(textA.split("\\s+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(textB.split("\\s+")));
        wordsA.retainAll(wordsB);
        if (wordsA.isEmpty()) return "High text similarity";
        return "Shared terms: " + String.join(", ", wordsA.stream().limit(5).toList());
    }

    private double cosineSimilarity(String a, String b) {
        if (a.isBlank() || b.isBlank()) return 0.0;
        String[] wordsA = a.split("\\s+");
        String[] wordsB = b.split("\\s+");

        Set<String> vocab = new HashSet<>(Arrays.asList(wordsA));
        vocab.addAll(Arrays.asList(wordsB));
        if (vocab.isEmpty()) return 0.0;

        double[] vecA = tfVector(Arrays.asList(wordsA), vocab);
        double[] vecB = tfVector(Arrays.asList(wordsB), vocab);

        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dot += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double[] tfVector(List<String> words, Set<String> vocab) {
        Map<String, Double> tf = new HashMap<>();
        for (String w : words) tf.merge(w, 1.0, Double::sum);
        double[] vec = new double[vocab.size()];
        int i = 0;
        for (String term : vocab) vec[i++] = tf.getOrDefault(term, 0.0);
        return vec;
    }
}
