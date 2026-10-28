package com.bnp.kingmainandhiskingdom.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercise B8 task 5: the API trusts two issuers. No server is needed: we play the identity provider ourselves
 * with our own RSA key pair.
 */
class MultiIssuerAuthenticationTest {

    private static final String IDP = "http://localhost:9000";

    private SecretKey kingdomKey;
    private JwtEncoder kingdomEncoder;
    private JwtEncoder idpEncoder;
    private AuthenticationManagerResolver<HttpServletRequest> resolver;

    @BeforeEach
    void setUp() throws Exception {
        kingdomKey = new SecretKeySpec("a-test-secret-of-at-least-32-characters!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        kingdomEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(kingdomKey));
        JwtDecoder kingdomDecoder = NimbusJwtDecoder.withSecretKey(kingdomKey).macAlgorithm(MacAlgorithm.HS256).build();

        KeyPair idpKeys = rsa();
        idpEncoder = rsaEncoder(idpKeys);
        NimbusJwtDecoder idpDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) idpKeys.getPublic()).build();
        idpDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(IDP));

        resolver = MultiIssuerAuthentication.resolver(kingdomDecoder, new JwtConfig().jwtAuthenticationConverter(), IDP, idpDecoder);
    }

    @Test
    void aTokenOfTheKingdomIsAccepted_withTheRolesClaim() {
        Authentication result = authenticate(kingdomEncoder, MacAlgorithm.HS256, JwtService.ISSUER, "king",
                List.of("KING"), null, null);

        assertEquals("king", result.getName());
        assertEquals(Set.of("ROLE_KING"), authorities(result));
    }

    @Test
    void aTokenOfTheIdentityProviderIsAccepted_andTheGroupsBecomeRoles() {
        Authentication result = authenticate(idpEncoder, SignatureAlgorithm.RS256, IDP, "uuid-123", null,
                List.of("guards"), "bob");

        assertEquals("bob", result.getName());
        assertEquals(Set.of("ROLE_KNIGHT"), authorities(result));
    }

    @Test
    void aTokenFromAnUnknownIssuerIsRefused() {
        assertThrows(AuthenticationException.class, () ->
                authenticate(idpEncoder, SignatureAlgorithm.RS256, "http://evil.example", "x", null, List.of("royal-court"), "evil"));
    }

    @Test
    void aTokenThatClaimsToBeFromTheIdpButIsSignedWithAnotherKeyIsRefused() throws Exception {
        JwtEncoder forger = rsaEncoder(rsa());
        assertThrows(AuthenticationException.class, () ->
                authenticate(forger, SignatureAlgorithm.RS256, IDP, "x", null, List.of("royal-court"), "forger"));
    }

    @Test
    void aTokenThatClaimsToBeFromTheIdpButUsesTheKingdomSecretIsRefused() {
        // someone who knows the shared secret of the kingdom still cannot pose as the identity provider
        assertThrows(AuthenticationException.class, () ->
                authenticate(kingdomEncoder, MacAlgorithm.HS256, IDP, "x", null, List.of("royal-court"), "forger"));
    }

    @Test
    void aTokenThatClaimsToBeFromTheKingdomButUsesAnRsaKeyIsRefused() throws Exception {
        assertThrows(AuthenticationException.class, () ->
                authenticate(rsaEncoder(rsa()), SignatureAlgorithm.RS256, JwtService.ISSUER, "x", List.of("KING"), null, null));
    }

    // ---- helpers -------------------------------------------------------------------------------

    private Authentication authenticate(JwtEncoder encoder, org.springframework.security.oauth2.jose.jws.JwsAlgorithm algorithm,
                                        String issuer, String subject, List<String> roles, List<String> groups, String username) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer).subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (roles != null) {
            claims.claim("roles", roles);
        }
        if (groups != null) {
            claims.claim("groups", groups);
        }
        if (username != null) {
            claims.claim("preferred_username", username);
        }
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), claims.build())).getTokenValue();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return resolver.resolve(request).authenticate(new BearerTokenAuthenticationToken(token));
    }

    /** Only the roles: Spring Security 7 adds authorities of its own, like FACTOR_BEARER. */
    private static Set<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).collect(Collectors.toSet());
    }

    private static KeyPair rsa() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static JwtEncoder rsaEncoder(KeyPair keys) {
        RSAKey key = new RSAKey.Builder((RSAPublicKey) keys.getPublic()).privateKey((RSAPrivateKey) keys.getPrivate()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
}
