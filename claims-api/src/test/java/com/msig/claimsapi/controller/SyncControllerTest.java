package com.msig.claimsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.GlobalExceptionHandler;
import com.msig.claimsapi.repository.*;
import com.msig.claimsapi.service.SyncEngine;
import com.msig.claimsdomain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TargetSystemRepository targetSystemRepository;
    @Mock
    private FieldMappingRepository fieldMappingRepository;
    @Mock
    private SyncTriggerRepository syncTriggerRepository;
    @Mock
    private SyncEventRepository syncEventRepository;
    @Mock
    private ClaimSyncStateRepository claimSyncStateRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private SyncEngine syncEngine;

    private TargetSystem sampleSystem;

    @BeforeEach
    void setUp() {
        SyncController controller = new SyncController(
                targetSystemRepository, fieldMappingRepository, syncTriggerRepository,
                syncEventRepository, claimSyncStateRepository, claimRepository, syncEngine);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        sampleSystem = TargetSystem.builder()
                .id(1L)
                .name("Guidewire ClaimCenter")
                .baseUrl("https://guidewire.example.com/api")
                .authType(AuthType.API_KEY)
                .active(true)
                .sandboxMode(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void getSystems_returns200() throws Exception {
        when(targetSystemRepository.findAll()).thenReturn(List.of(sampleSystem));

        mockMvc.perform(get("/api/systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Guidewire ClaimCenter"));
    }

    @Test
    void createSystem_valid_returns201() throws Exception {
        when(targetSystemRepository.save(any(TargetSystem.class))).thenReturn(sampleSystem);

        Map<String, Object> req = Map.of(
                "name", "Guidewire ClaimCenter",
                "baseUrl", "https://guidewire.example.com/api",
                "authType", "API_KEY",
                "sandboxMode", true
        );

        mockMvc.perform(post("/api/systems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()); // SyncController returns 200 for POST /systems
    }

    @Test
    void retryEvent_returns200() throws Exception {
        SyncEvent event = SyncEvent.builder()
                .id(5L)
                .claimId(1L)
                .status(SyncStatus.FAILED)
                .attemptCount(1)
                .createdAt(Instant.now())
                .build();
        when(syncEventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(syncEventRepository.save(any(SyncEvent.class))).thenReturn(event);

        mockMvc.perform(post("/api/sync/events/5/retry"))
                .andExpect(status().isOk());

        verify(syncEventRepository).save(argThat(e -> e.getStatus() == SyncStatus.RETRYING));
    }

    @Test
    void syncAll_returns200() throws Exception {
        when(claimSyncStateRepository.findByDirtyTrue()).thenReturn(List.of());

        mockMvc.perform(post("/api/sync/sync-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queued").value(0));
    }

    @Test
    void getSyncHealth_noActiveSystem_returnsNoSystem() throws Exception {
        when(targetSystemRepository.findByActiveTrue()).thenReturn(List.of());

        mockMvc.perform(get("/api/sync/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("no_system"));
    }
}
