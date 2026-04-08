package com.msig.claimsapi.service.ai;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.model.*;

import java.util.List;

public interface AIOrchestrationService {
    
    ProcessingResult<ExtractedClaimData> extractClaimData(FNOLDocument fnolDocument);
    
    ProcessingResult<PolicyVerificationResult> verifyPolicy(Claim claim, Policy policy);
    
    ProcessingResult<EntityMatchResult> matchEntities(ExtractedEntities extractedEntities);
    
    ProcessingResult<ForensicsResult> runForensics(List<Evidence> evidence);
    
    ProcessingResult<List<DuplicateMatch>> detectDuplicates(Claim claim);
    
    boolean isAIAvailable();
    
    void recordSuccess();
    
    void recordFailure();
}