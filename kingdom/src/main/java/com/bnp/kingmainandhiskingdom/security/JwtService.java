package com.bnp.kingmainandhiskingdom.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
public class JwtService {

    public static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final KingdomSecurityProperties properties;

    public JwtService(JwtEncoder encoder, KingdomSecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken issue(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")) // Spring Security 7 also adds authorities like FACTOR_PASSWORD
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList();
        return issue(authentication.getName(), roles);
    }

    /** Role names without the ROLE_ prefix. */
    public IssuedToken issue(String subject, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.jwtTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("king-main-and-his-kingdom")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLES_CLAIM, roles)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new IssuedToken(token, expiresAt, List.copyOf(roles));
    }

    public record IssuedToken(String token, Instant expiresAt, List<String> roles) {
    }
}
