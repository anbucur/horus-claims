package com.msig.claimsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ClaimsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsApiApplication.class, args);
    }
}
