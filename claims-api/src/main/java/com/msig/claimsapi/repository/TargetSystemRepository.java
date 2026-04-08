package com.msig.claimsapi.repository;

import com.msig.claimsdomain.model.TargetSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TargetSystemRepository extends JpaRepository<TargetSystem, Long> {
    List<TargetSystem> findByActiveTrue();
}
