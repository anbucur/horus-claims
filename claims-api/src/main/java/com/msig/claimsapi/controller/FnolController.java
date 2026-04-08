package com.msig.claimsapi.controller;

import com.msig.claimsapi.dto.FnolIntakeRequest;
import com.msig.claimsapi.dto.FnolIntakeResponse;
import com.msig.claimsapi.service.FnolIntakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fnol")
@RequiredArgsConstructor
@Slf4j
public class FnolController {

    private final FnolIntakeService fnolIntakeService;

    @PostMapping("/intake")
    public ResponseEntity<FnolIntakeResponse> intake(@RequestBody FnolIntakeRequest request) {
        log.info("POST /api/fnol/intake - FNOL intake for policy {}", request.getPolicyNumber());
        FnolIntakeResponse response = fnolIntakeService.submitIntake(request);
        return ResponseEntity.ok(response);
    }
}
