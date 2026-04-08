package com.msig.claimsapi.controller;

import com.msig.claimsapi.dto.FnolIntakeRequest;
import com.msig.claimsapi.dto.FnolIntakeResponse;
import com.msig.claimsapi.service.FnolIntakeService;
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

@RestController
@RequestMapping("/api/fnol")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FNOL", description = "First Notice of Loss intake")
public class FnolController {

    private final FnolIntakeService fnolIntakeService;

    @PostMapping("/intake")
    @Operation(summary = "Submit a First Notice of Loss (FNOL) intake")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FNOL submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or policy not found")
    })
    public ResponseEntity<FnolIntakeResponse> intake(
            @Parameter(description = "FNOL intake request") @Valid @RequestBody FnolIntakeRequest request) {
        log.info("POST /api/fnol/intake - FNOL intake for policy {}", request.getPolicyNumber());
        FnolIntakeResponse response = fnolIntakeService.submitIntake(request);
        return ResponseEntity.ok(response);
    }
}
