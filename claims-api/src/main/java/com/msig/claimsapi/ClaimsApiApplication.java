package com.msig.claimsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = {"com.msig.claimsdomain.entities", "com.msig.claimsdomain.model"})
public class ClaimsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsApiApplication.class, args);
    }
}
