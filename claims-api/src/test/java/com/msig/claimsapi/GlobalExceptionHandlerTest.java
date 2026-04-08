package com.msig.claimsapi;

import com.msig.claimsapi.config.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid claim ID"));
    }

    @Test
    void entityNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test/entity-not-found")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void illegalState_returns409() throws Exception {
        mockMvc.perform(get("/test/illegal-state")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    /** Minimal controller that throws the exception types we want to test. */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid claim ID");
        }

        @GetMapping("/entity-not-found")
        public void throwEntityNotFound() {
            throw new EntityNotFoundException("Claim 999 not found");
        }

        @GetMapping("/illegal-state")
        public void throwIllegalState() {
            throw new IllegalStateException("Invalid workflow transition");
        }
    }
}
