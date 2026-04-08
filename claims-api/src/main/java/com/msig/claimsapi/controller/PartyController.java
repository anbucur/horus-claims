package com.msig.claimsapi.controller;

import com.msig.claimsapi.service.PartyService;
import com.msig.claimsdomain.entities.Party;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parties")
@RequiredArgsConstructor
@Slf4j
public class PartyController {

    private final PartyService partyService;

    @GetMapping
    public ResponseEntity<List<Party>> getAllParties() {
        log.debug("GET /api/parties - retrieving all parties");
        return ResponseEntity.ok(partyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Party> getPartyById(@PathVariable("id") Long id) {
        log.debug("GET /api/parties/{} - retrieving party", id);
        return partyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Party> getPartyByEmail(@PathVariable String email) {
        log.debug("GET /api/parties/email/{} - retrieving party by email", email);
        return partyService.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Party> createParty(@RequestBody Party party) {
        log.info("POST /api/parties - creating new party");
        Party saved = partyService.save(party);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Party> updateParty(@PathVariable("id") Long id, @RequestBody Party party) {
        log.info("PUT /api/parties/{} - updating party", id);
        if (!id.equals(party.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(partyService.save(party));
    }

    @PatchMapping("/{id}/contact")
    public ResponseEntity<Party> updateContactInfo(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        log.info("PATCH /api/parties/{}/contact - updating contact info", id);
        return ResponseEntity.ok(partyService.updateContactInfo(
                id,
                body.get("email"),
                body.get("phone"),
                body.get("address")
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParty(@PathVariable("id") Long id) {
        log.info("DELETE /api/parties/{} - deleting party", id);
        partyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}