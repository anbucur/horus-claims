package com.msig.claimsapi.repository;

import com.msig.claimsdomain.model.FieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FieldMappingRepository extends JpaRepository<FieldMapping, Long> {
    List<FieldMapping> findByTargetSystemIdAndActiveTrueOrderBySortOrderAsc(Long targetSystemId);
    List<FieldMapping> findByTargetSystemId(Long targetSystemId);
}
