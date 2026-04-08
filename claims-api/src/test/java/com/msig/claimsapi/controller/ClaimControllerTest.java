package com.msig.claimsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.msig.claimsapi.config.GlobalExceptionHandler;
import com.msig.claimsapi.service.ClaimService;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ClaimService claimService;

    @Mock
    private ClaimAuditService auditService;

    private Claim sampleClaim;

    @BeforeEach
    void setUp() {
        ClaimController controller = new ClaimController(claimService, auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Policy policy = Policy.builder()
                .id(1L)
                .policyNumber("POL-001")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .expirationDate(LocalDate.of(2025, 1, 1))
                .lineOfBusiness("Marine Hull")
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        sampleClaim = Claim.builder()
                .id(1L)
                .policy(policy)
                .dateOfLoss(LocalDate.of(2024, 3, 15))
                .incidentNarrative("Cargo damage during voyage")
                .lossLocation("Port of Singapore")
                .workflowStatus(WorkflowStatus.RECEIVED)
                .build();
    }

    @Test
    void getAll_returnsOk() throws Exception {
        when(claimService.findAll()).thenReturn(List.of(sampleClaim));

        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(claimService.findById(1L)).thenReturn(Optional.of(sampleClaim));

        mockMvc.perform(get("/api/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.workflowStatus").value("RECEIVED"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(claimService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/claims/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(claimService.save(any(Claim.class))).thenReturn(sampleClaim);

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleClaim)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateStatus_validTransition_returns200() throws Exception {
        Claim updated = Claim.builder()
                .id(1L)
                .workflowStatus(WorkflowStatus.EXTRACTING)
                .build();
        when(claimService.updateWorkflowStatus(eq(1L), eq(WorkflowStatus.EXTRACTING))).thenReturn(updated);

        mockMvc.perform(patch("/api/claims/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "EXTRACTING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("EXTRACTING"));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        doNothing().when(claimService).deleteById(1L);

        mockMvc.perform(delete("/api/claims/1"))
                .andExpect(status().isNoContent());

        verify(claimService).deleteById(1L);
    }
}
