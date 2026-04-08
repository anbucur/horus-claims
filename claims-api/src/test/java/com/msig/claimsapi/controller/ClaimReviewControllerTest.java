package com.msig.claimsapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.GlobalExceptionHandler;
import com.msig.claimsapi.dto.SubmitReviewRequest;
import com.msig.claimsapi.repository.ClaimReviewRepository;
import com.msig.claimsapi.service.ClaimReviewService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.ClaimReview;
import com.msig.claimsdomain.model.ClaimReviewAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClaimReviewControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ClaimReviewService reviewService;

    @Mock
    private ClaimReviewRepository reviewRepository;

    private ClaimReview sampleReview;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ClaimReviewController controller = new ClaimReviewController(reviewService, reviewRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();

        Claim claim = Claim.builder().id(1L).build();
        sampleReview = ClaimReview.builder()
                .id(10L)
                .claim(claim)
                .action(ClaimReviewAction.APPROVE)
                .reviewer("john.doe@msig.com")
                .reviewerNotes("All documents verified")
                .build();
    }

    @Test
    void submitReview_valid_returns200() throws Exception {
        SubmitReviewRequest request = SubmitReviewRequest.builder()
                .action("APPROVE")
                .reviewer("john.doe@msig.com")
                .notes("All documents verified")
                .build();

        when(reviewService.submitReview(eq(1L), eq(ClaimReviewAction.APPROVE), anyString(), anyString()))
                .thenReturn(sampleReview);

        mockMvc.perform(post("/api/claims/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void submitReview_missingReviewer_returns400() throws Exception {
        SubmitReviewRequest request = SubmitReviewRequest.builder()
                .action("APPROVE")
                .notes("Some notes")
                // reviewer intentionally omitted
                .build();

        mockMvc.perform(post("/api/claims/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReviews_returns200() throws Exception {
        when(reviewRepository.findByClaimIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(sampleReview));

        mockMvc.perform(get("/api/claims/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }
}
