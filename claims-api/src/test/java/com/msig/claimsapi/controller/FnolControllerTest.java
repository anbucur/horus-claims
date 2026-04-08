package com.msig.claimsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.msig.claimsapi.config.GlobalExceptionHandler;
import com.msig.claimsapi.dto.FnolIntakeRequest;
import com.msig.claimsapi.dto.FnolIntakeResponse;
import com.msig.claimsapi.service.FnolIntakeService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FnolControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FnolIntakeService fnolIntakeService;

    @BeforeEach
    void setUp() {
        FnolController controller = new FnolController(fnolIntakeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void intake_validRequest_returns200() throws Exception {
        FnolIntakeRequest request = FnolIntakeRequest.builder()
                .policyNumber("POL-001")
                .dateOfLoss(LocalDate.of(2024, 1, 15))
                .incidentNarrative("Cargo damage during voyage")
                .lossLocation("Port of Singapore")
                .submittedBy("intake-agent")
                .evidenceUrls(List.of("https://storage.example.com/doc1.pdf"))
                .build();

        FnolIntakeResponse response = FnolIntakeResponse.builder()
                .claimId(100L)
                .status("RECEIVED")
                .policyNumber("POL-001")
                .message("Claim received successfully")
                .traceId(UUID.randomUUID().toString())
                .build();

        when(fnolIntakeService.submitIntake(any(FnolIntakeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/fnol/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(100))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.policyNumber").value("POL-001"));
    }

    @Test
    void intake_policyNotFound_returns400() throws Exception {
        FnolIntakeRequest request = FnolIntakeRequest.builder()
                .policyNumber("INVALID-POL")
                .dateOfLoss(LocalDate.of(2024, 1, 15))
                .submittedBy("intake-agent")
                .build();

        when(fnolIntakeService.submitIntake(any(FnolIntakeRequest.class)))
                .thenThrow(new IllegalArgumentException("Policy not found: INVALID-POL"));

        mockMvc.perform(post("/api/fnol/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void intake_minimalRequest_returns200() throws Exception {
        FnolIntakeRequest request = FnolIntakeRequest.builder()
                .policyNumber("POL-001")
                .dateOfLoss(LocalDate.now())
                .submittedBy("intake-agent")
                .build();

        FnolIntakeResponse response = FnolIntakeResponse.builder()
                .claimId(101L)
                .status("RECEIVED")
                .policyNumber("POL-001")
                .message("Claim received successfully")
                .traceId(UUID.randomUUID().toString())
                .build();

        when(fnolIntakeService.submitIntake(any(FnolIntakeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/fnol/intake")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(101));
    }
}
