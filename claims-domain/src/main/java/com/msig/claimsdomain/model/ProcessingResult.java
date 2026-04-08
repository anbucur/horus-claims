package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProcessingResult<T> {
    
    private T data;
    private ProcessingMode mode;
    @Builder.Default
    private boolean aiAvailable = true;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    private String aiModelUsed;
    @Builder.Default
    private String traceId = UUID.randomUUID().toString();
    
    public static <T> ProcessingResult<T> empty(ProcessingMode mode) {
        return ProcessingResult.<T>builder()
                .mode(mode)
                .aiAvailable(false)
                .warnings(List.of("AI processing not available"))
                .build();
    }
    
    public static <T> ProcessingResult<T> manualFallback(T data, ProcessingMode mode) {
        return ProcessingResult.<T>builder()
                .data(data)
                .mode(mode)
                .aiAvailable(false)
                .warnings(List.of("Falling back to manual processing"))
                .build();
    }
    
    public static <T> ProcessingResult<T> success(T data, ProcessingMode mode, String aiModel) {
        return ProcessingResult.<T>builder()
                .data(data)
                .mode(mode)
                .aiAvailable(true)
                .aiModelUsed(aiModel)
                .build();
    }
}