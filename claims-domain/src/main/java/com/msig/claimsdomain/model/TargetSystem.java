package com.msig.claimsdomain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "target_systems")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type")
    private AuthType authType;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "oauth_client_id")
    private String oauthClientId;

    @Column(name = "oauth_client_secret")
    private String oauthClientSecret;

    @Column(name = "basic_username")
    private String basicUsername;

    @Column(name = "basic_password")
    private String basicPassword;

    @Column(columnDefinition = "text")
    private String endpoints;

    @Column(columnDefinition = "text")
    private String retryPolicy;

    @Column(name = "sandbox_mode")
    private Boolean sandboxMode;

    private Boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getMaskedApiKey() {
        if (apiKey == null || apiKey.isEmpty()) return null;
        return "****" + apiKey.substring(Math.max(0, apiKey.length() - 4));
    }
}
