package com.bnp.kingmainandhiskingdom.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Typed configuration, bound from the "kingdom.security" keys in application.yaml.
 *
 * @param jwtSecret          HMAC key to sign the JWTs (min. 32 characters). Override it with the
 *                           KINGDOM_SECURITY_JWT_SECRET environment variable outside of development!
 * @param jwtTtl             how long a token is valid
 * @param corsAllowedOrigins origins (protocol + host + port) that may call the API from a browser
 * @param apiKey             key for the machine-to-machine endpoints under /api/integration (min. 16 characters)
 */
@ConfigurationProperties("kingdom.security")
public record KingdomSecurityProperties(String jwtSecret, Duration jwtTtl, List<String> corsAllowedOrigins, String apiKey) {

    public KingdomSecurityProperties {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("kingdom.security.jwt-secret must be at least 32 characters long");
        }
        if (apiKey == null || apiKey.length() < 16) {
            throw new IllegalStateException("kingdom.security.api-key must be at least 16 characters long");
        }
        if (jwtTtl == null) {
            jwtTtl = Duration.ofHours(1);
        }
        if (corsAllowedOrigins == null) {
            corsAllowedOrigins = List.of();
        }
    }
}
