package com.msig.claimsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.GlobalExceptionHandler;
import com.msig.claimsapi.config.ProcessingConfig;
import com.msig.claimsapi.service.ClaimProcessingService;
import com.msig.claimsapi.service.ClaimService;
import com.msig.claimsapi.service.ai.AICircuitBreaker;
import com.msig.claimsapi.service.ai.AIOrchestrationService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.model.ProcessingMode;
import com.msig.claimsdomain.model.ProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaimWorkflowControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ClaimService claimService;

    @Mock
    private ClaimProcessingService claimProcessingService;

    @Mock
    private ProcessingConfig processingConfig;

    @Mock
    private AIOrchestrationService aiOrchestrationService;

    @Mock
    private AICircuitBreaker circuitBreaker;

    private Claim sampleClaim;

    @BeforeEach
    void setUp() {
        ClaimWorkflowController controller = new ClaimWorkflowController(
                claimService, claimProcessingService, processingConfig, aiOrchestrationService, circuitBreaker);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        sampleClaim = Claim.builder()
                .id(1L)
                .workflowStatus(WorkflowStatus.VERIFYING)
                .aiConfidenceScore(88.0)
                .build();
    }

    @Test
    void processClaim_validId_returns200() throws Exception {
        when(claimService.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(processingConfig.getDefaultMode()).thenReturn(ProcessingMode.AI_ASSISTED);
        ProcessingResult<Claim> result = ProcessingResult.<Claim>builder()
                .data(sampleClaim)
                .mode(ProcessingMode.AI_ASSISTED)
                .aiAvailable(true)
                .warnings(List.of())
                .traceId("trace-001")
                .build();
        when(claimProcessingService.processClaim(1L, ProcessingMode.AI_ASSISTED)).thenReturn(result);

        mockMvc.perform(post("/api/claims/1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim_id").value(1));
    }

    @Test
    void setMode_validMode_returns200() throws Exception {
        when(claimService.findById(1L)).thenReturn(Optional.of(sampleClaim));

        mockMvc.perform(post("/api/claims/1/set-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("mode", "FULL_MANUAL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim_id").value(1));
    }

    @Test
    void getProcessingStatus_returns200() throws Exception {
        when(claimService.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(processingConfig.getDefaultMode()).thenReturn(ProcessingMode.AI_ASSISTED);
        when(processingConfig.getStpConfidenceThreshold()).thenReturn(85);
        when(processingConfig.getHitlConfidenceThreshold()).thenReturn(60);
        when(aiOrchestrationService.isAIAvailable()).thenReturn(true);
        when(circuitBreaker.getState()).thenReturn(AICircuitBreaker.State.CLOSED);

        mockMvc.perform(get("/api/claims/1/processing-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim_id").value(1))
                .andExpect(jsonPath("$.ai_circuit_breaker_state").value("CLOSED"));
    }

    @Test
    void overrideClaim_returns200() throws Exception {
        when(claimService.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(claimService.save(any(Claim.class))).thenReturn(sampleClaim);

        mockMvc.perform(post("/api/claims/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "APPROVED",
                                "reason", "Manual override by supervisor"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claim_id").value(1));
    }
}
