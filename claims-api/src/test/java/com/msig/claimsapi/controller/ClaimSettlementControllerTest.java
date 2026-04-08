package com.msig.claimsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.msig.claimsapi.dto.ApproveReserveRequest;
import com.msig.claimsapi.dto.CreateReserveRequest;
import com.msig.claimsapi.service.settlement.ClaimCompletionService;
import com.msig.claimsapi.service.settlement.ClaimSettlementService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Financials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaimSettlementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ClaimSettlementService settlementService;

    @Mock
    private ClaimCompletionService completionService;

    @Mock
    private com.msig.claimsapi.service.settlement.SettlementLetterService settlementLetterService;

    private Claim sampleClaim;
    private Financials sampleReserve;

    @BeforeEach
    void setUp() {
        ClaimSettlementController controller = new ClaimSettlementController(settlementService, completionService, settlementLetterService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleClaim = Claim.builder()
                .id(1L)
                .workflowStatus(WorkflowStatus.STP)
                .build();

        sampleReserve = Financials.builder()
                .id(10L)
                .claim(sampleClaim)
                .category(Financials.Category.INDEMNITY)
                .amount(new BigDecimal("50000.00"))
                .currency("USD")
                .status(Financials.TransactionStatus.RESERVE)
                .transactionDate(LocalDate.now())
                .build();
    }

    @Test
    void createReserve_validRequest_returnsOk() throws Exception {
        when(settlementService.createReserve(eq(1L), any(BigDecimal.class), eq("USD"),
                eq(Financials.Category.INDEMNITY), eq("adjuster1")))
                .thenReturn(sampleReserve);

        CreateReserveRequest body = new CreateReserveRequest(
                new BigDecimal("50000.00"), "USD",
                Financials.Category.INDEMNITY, "adjuster1"
        );

        mockMvc.perform(post("/api/claims/1/reserves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getReserves_returnsReserveList() throws Exception {
        when(settlementService.getReserves(1L)).thenReturn(List.of(sampleReserve));

        mockMvc.perform(get("/api/claims/1/reserves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("RESERVE"));
    }

    @Test
    void getSettlementSummary_returnsSummary() throws Exception {
        ClaimSettlementService.SettlementSummary summary =
                new ClaimSettlementService.SettlementSummary(
                        new BigDecimal("50000.00"),
                        new BigDecimal("45000.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("45000.00")
                );

        when(settlementService.getSettlementSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/claims/1/settlement-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReserveAmount").value(50000.00))
                .andExpect(jsonPath("$.totalApprovedAmount").value(45000.00))
                .andExpect(jsonPath("$.netAmountOutstanding").value(45000.00));
    }

    @Test
    void approveReserve_validRequest_returnsPayment() throws Exception {
        Financials payment = Financials.builder()
                .id(20L)
                .claim(sampleClaim)
                .category(Financials.Category.INDEMNITY)
                .amount(new BigDecimal("48000.00"))
                .currency("USD")
                .status(Financials.TransactionStatus.PAYMENT)
                .approvedBy("manager1")
                .approvedAt(LocalDateTime.now())
                .build();

        when(settlementService.approveReserve(eq(10L), any(BigDecimal.class), eq("manager1")))
                .thenReturn(payment);

        ApproveReserveRequest body = new ApproveReserveRequest(10L, new BigDecimal("48000.00"), "manager1");

        mockMvc.perform(post("/api/claims/1/approve-reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.status").value("PAYMENT"))
                .andExpect(jsonPath("$.approvedBy").value("manager1"));
    }

    @Test
    void settle_validRequest_returnsCompletedClaim() throws Exception {
        Claim completedClaim = Claim.builder()
                .id(1L)
                .workflowStatus(WorkflowStatus.COMPLETED)
                .settlementAmount(new BigDecimal("48000.00"))
                .settlementCurrency("USD")
                .settledAt(LocalDateTime.now())
                .build();

        when(settlementService.processPayment(eq(1L), eq("settlement-clerk")))
                .thenReturn(completedClaim);

        Map<String, String> body = Map.of("performedBy", "settlement-clerk");

        mockMvc.perform(post("/api/claims/1/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.settlementAmount").value(48000.00));
    }

    @Test
    void reject_validRequest_returnsRejectedClaim() throws Exception {
        Claim rejectedClaim = Claim.builder()
                .id(1L)
                .workflowStatus(WorkflowStatus.REJECTED)
                .rejectionReason("Insufficient documentation")
                .build();

        when(settlementService.rejectSettlement(eq(1L), eq("Insufficient documentation"), eq("manager1")))
                .thenReturn(rejectedClaim);

        Map<String, String> body = Map.of(
                "reason", "Insufficient documentation",
                "performedBy", "manager1"
        );

        mockMvc.perform(post("/api/claims/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Insufficient documentation"));
    }

    @Test
    void getSettlementCalculation_returnsCalculation() throws Exception {
        ClaimCompletionService.SettlementCalculation calc =
                new ClaimCompletionService.SettlementCalculation(
                        new BigDecimal("50000.00"),
                        new BigDecimal("48000.00"),
                        new BigDecimal("48000.00"),
                        BigDecimal.ZERO,
                        "USD"
                );

        when(completionService.calculateSettlement(1L)).thenReturn(calc);

        mockMvc.perform(get("/api/claims/1/settlement-calculation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReserves").value(50000.00))
                .andExpect(jsonPath("$.totalApproved").value(48000.00));
    }
}
