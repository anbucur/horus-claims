package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExtractedEntities {
    private List<String> assuredNames;
    private List<String> brokerNames;
    private List<String> vesselNames;
    private List<String> imoNumbers;
}