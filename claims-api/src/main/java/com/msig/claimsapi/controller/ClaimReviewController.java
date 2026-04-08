package com.msig.claimsapi.controller;

import com.msig.claimsapi.dto.SubmitReviewRequest;
import com.msig.claimsapi.repository.ClaimReviewRepository;
import com.msig.claimsapi.service.ClaimReviewService;
import com.msig.claimsdomain.entities.ClaimReview;
import com.msig.claimsdomain.model.ClaimReviewAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review", description = "Human-in-the-loop claim review")
public class ClaimReviewController {

    private final ClaimReviewService reviewService;
    private final ClaimReviewRepository reviewRepository;

    @PostMapping("/{id}/review")
    @Operation(summary = "Submit a human review decision for a claim")
    @ApiResponse(responseCode = "200", description = "Review submitted")
    public ResponseEntity<ClaimReview> submitReview(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id,
            @Parameter(description = "Review details") @Valid @RequestBody SubmitReviewRequest request) {
        log.info("POST /api/claims/{}/review", id);
        ClaimReviewAction action = ClaimReviewAction.valueOf(request.getAction());
        ClaimReview saved = reviewService.submitReview(id, action, request.getNotes(), request.getReviewer());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get all reviews for a claim")
    @ApiResponse(responseCode = "200", description = "List of reviews")
    public ResponseEntity<List<ClaimReview>> getReviews(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id) {
        log.info("GET /api/claims/{}/reviews", id);
        return ResponseEntity.ok(reviewRepository.findByClaimIdOrderByCreatedAtDesc(id));
    }
}
