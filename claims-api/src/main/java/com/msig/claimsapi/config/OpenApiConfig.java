package com.msig.claimsapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI horusOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Horus Claims API")
                        .description("Marine Insurance Claims Processing Platform API")
                        .version("1.0.0")
                        .contact(new Contact().name("Horus Dev Team")));
    }
}
