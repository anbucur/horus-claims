package com.msig.claimsapi.service;

import com.msig.claimsapi.repository.PartyRepository;
import com.msig.claimsdomain.entities.Party;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartyService {

    private final PartyRepository partyRepository;

    @Transactional(readOnly = true)
    public List<Party> findAll() {
        return partyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Party> findById(Long id) {
        return partyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Party> findByEmail(String email) {
        return partyRepository.findByEmail(email);
    }

    @Transactional
    public Party save(Party party) {
        log.debug("Saving party: {}", party.getName());
        return partyRepository.save(party);
    }

    @Transactional
    public void deleteById(Long id) {
        log.debug("Deleting party: {}", id);
        partyRepository.deleteById(id);
    }

    @Transactional
    public Party updateContactInfo(Long partyId, String email, String phone, String address) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party not found: " + partyId));
        if (email != null) {
            party.setEmail(email);
        }
        if (phone != null) {
            party.setPhone(phone);
        }
        if (address != null) {
            party.setAddress(address);
        }
        return partyRepository.save(party);
    }
}