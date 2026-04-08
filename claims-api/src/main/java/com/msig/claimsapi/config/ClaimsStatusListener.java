package com.msig.claimsapi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClaimsStatusListener {

    @KafkaListener(
            topics = "claims-status",
            groupId = "claims-api-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleClaimStatusUpdate(@Payload String message) {
        log.info("Received claims-status update: {}", message);
        try {
            log.debug("Processing status update message");
        } catch (Exception e) {
            log.error("Error processing claims-status message: {}", e.getMessage(), e);
        }
    }
}