package com.msig.claimsapi.controller;

import com.msig.claimsapi.repository.ClaimReviewRepository;
import com.msig.claimsapi.service.ClaimReviewService;
import com.msig.claimsdomain.entities.ClaimReview;
import com.msig.claimsdomain.model.ClaimReviewAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
public class ClaimReviewController {

    private final ClaimReviewService reviewService;
    private final ClaimReviewRepository reviewRepository;

    @PostMapping("/{id}/review")
    public ResponseEntity<ClaimReview> submitReview(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("POST /api/claims/{}/review", id);
        ClaimReviewAction action = ClaimReviewAction.valueOf(body.get("action"));
        String notes = body.get("notes");
        String reviewer = body.get("reviewer");
        ClaimReview saved = reviewService.submitReview(id, action, notes, reviewer);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ClaimReview>> getReviews(@PathVariable Long id) {
        log.info("GET /api/claims/{}/reviews", id);
        return ResponseEntity.ok(reviewRepository.findByClaimIdOrderByCreatedAtDesc(id));
    }
}
