package com.msig.claimsapi.controller;

import com.msig.claimsapi.dto.ApproveReserveRequest;
import com.msig.claimsapi.dto.CreateReserveRequest;
import com.msig.claimsapi.service.settlement.ClaimCompletionService;
import com.msig.claimsapi.service.settlement.ClaimSettlementService;
import com.msig.claimsapi.service.settlement.SettlementLetterService;
import com.msig.claimsapi.service.settlement.ClaimCompletionService.SettlementCalculation;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Financials;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settlement", description = "Claims settlement and payment processing")
@Validated
public class ClaimSettlementController {

    private final ClaimSettlementService settlementService;
    private final ClaimCompletionService completionService;
    private final SettlementLetterService settlementLetterService;

    @PostMapping("/{id}/reserves")
    @Operation(summary = "Create a financial reserve for a claim")
    @ApiResponse(responseCode = "200", description = "Reserve created")
    public ResponseEntity<Financials> createReserve(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id,
            @Parameter(description = "Reserve details") @Valid @RequestBody CreateReserveRequest request) {
        log.info("POST /api/claims/{}/reserves - creating reserve", id);
        Financials reserve = settlementService.createReserve(
                id,
                request.getAmount(),
                request.getCurrency(),
                request.getCategory(),
                request.getPerformedBy()
        );
        return ResponseEntity.ok(reserve);
    }

    @GetMapping("/{id}/reserves")
    @Operation(summary = "Get all reserves for a claim")
    @ApiResponse(responseCode = "200", description = "List of reserves")
    public ResponseEntity<List<Financials>> getReserves(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id) {
        log.info("GET /api/claims/{}/reserves", id);
        return ResponseEntity.ok(settlementService.getReserves(id));
    }

    @GetMapping("/{id}/settlement-summary")
    @Operation(summary = "Get settlement summary for a claim")
    @ApiResponse(responseCode = "200", description = "Settlement summary")
    public ResponseEntity<ClaimSettlementService.SettlementSummary> getSettlementSummary(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id) {
        log.info("GET /api/claims/{}/settlement-summary", id);
        return ResponseEntity.ok(settlementService.getSettlementSummary(id));
    }

    @PostMapping("/{id}/approve-reserve")
    @Operation(summary = "Approve a reserve and convert to payment")
    @ApiResponse(responseCode = "200", description = "Reserve approved as payment")
    public ResponseEntity<Financials> approveReserve(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id,
            @Parameter(description = "Approval details") @Valid @RequestBody ApproveReserveRequest request) {
        log.info("POST /api/claims/{}/approve-reserve", id);
        Financials payment = settlementService.approveReserve(
                request.getFinancialsId(),
                request.getApprovedAmount(),
                request.getPerformedBy()
        );
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "Process final settlement and mark claim as completed")
    @ApiResponse(responseCode = "200", description = "Settlement processed")
    public ResponseEntity<Claim> settle(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id,
            @Parameter(description = "Settlement details") @RequestBody java.util.Map<String, String> body) {
        log.info("POST /api/claims/{}/settle", id);
        Claim settled = settlementService.processPayment(id, body.get("performedBy"));
        return ResponseEntity.ok(settled);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a settlement")
    @ApiResponse(responseCode = "200", description = "Settlement rejected")
    public ResponseEntity<Claim> reject(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id,
            @Parameter(description = "Rejection reason") @RequestBody java.util.Map<String, String> body) {
        log.info("POST /api/claims/{}/reject", id);
        Claim rejected = settlementService.rejectSettlement(id, body.get("reason"), body.get("performedBy"));
        return ResponseEntity.ok(rejected);
    }

    @GetMapping("/{id}/settlement-calculation")
    @Operation(summary = "Get settlement calculation for a claim")
    @ApiResponse(responseCode = "200", description = "Settlement calculation")
    public ResponseEntity<SettlementCalculation> getSettlementCalculation(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id) {
        log.info("GET /api/claims/{}/settlement-calculation", id);
        return ResponseEntity.ok(completionService.calculateSettlement(id));
    }

    @GetMapping("/{id}/settlement-letter")
    @Operation(summary = "Generate settlement letter PDF for a completed/settled claim")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF settlement letter"),
            @ApiResponse(responseCode = "404", description = "Claim not found or not settled")
    })
    public ResponseEntity<byte[]> getSettlementLetter(@PathVariable("id") Long id) {
        log.info("GET /api/claims/{}/settlement-letter", id);
        byte[] pdfBytes = settlementLetterService.generateSettlementLetter(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=settlement-letter-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
