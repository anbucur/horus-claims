package com.msig.claimsapi.controller;

import com.msig.claimsapi.service.settlement.ClaimCompletionService;
import com.msig.claimsapi.service.settlement.ClaimSettlementService;
import com.msig.claimsapi.service.settlement.ClaimCompletionService.SettlementCalculation;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Financials;
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
public class ClaimSettlementController {

    private final ClaimSettlementService settlementService;
    private final ClaimCompletionService completionService;

    @PostMapping("/{id}/reserves")
    public ResponseEntity<Financials> createReserve(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("POST /api/claims/{}/reserves - creating reserve", id);
        Financials reserve = settlementService.createReserve(
                id,
                new java.math.BigDecimal(body.get("amount").toString()),
                (String) body.get("currency"),
                Financials.Category.valueOf((String) body.get("category")),
                (String) body.get("performedBy")
        );
        return ResponseEntity.ok(reserve);
    }

    @GetMapping("/{id}/reserves")
    public ResponseEntity<List<Financials>> getReserves(@PathVariable Long id) {
        log.info("GET /api/claims/{}/reserves", id);
        return ResponseEntity.ok(settlementService.getReserves(id));
    }

    @GetMapping("/{id}/settlement-summary")
    public ResponseEntity<ClaimSettlementService.SettlementSummary> getSettlementSummary(@PathVariable Long id) {
        log.info("GET /api/claims/{}/settlement-summary", id);
        return ResponseEntity.ok(settlementService.getSettlementSummary(id));
    }

    @PostMapping("/{id}/approve-reserve")
    public ResponseEntity<Financials> approveReserve(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("POST /api/claims/{}/approve-reserve", id);
        Financials payment = settlementService.approveReserve(
                Long.valueOf(body.get("financialsId").toString()),
                new java.math.BigDecimal(body.get("approvedAmount").toString()),
                (String) body.get("performedBy")
        );
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<Claim> settle(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("POST /api/claims/{}/settle", id);
        Claim settled = settlementService.processPayment(id, body.get("performedBy"));
        return ResponseEntity.ok(settled);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Claim> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("POST /api/claims/{}/reject", id);
        Claim rejected = settlementService.rejectSettlement(id, body.get("reason"), body.get("performedBy"));
        return ResponseEntity.ok(rejected);
    }

    @GetMapping("/{id}/settlement-calculation")
    public ResponseEntity<SettlementCalculation> getSettlementCalculation(@PathVariable Long id) {
        log.info("GET /api/claims/{}/settlement-calculation", id);
        return ResponseEntity.ok(completionService.calculateSettlement(id));
    }
}
