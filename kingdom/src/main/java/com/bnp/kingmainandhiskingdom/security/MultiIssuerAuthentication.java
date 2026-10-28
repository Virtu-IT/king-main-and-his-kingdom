package com.bnp.kingmainandhiskingdom.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;

import java.util.List;
import java.util.Map;

/**
 * Exercise B8 task 5: the API accepts access tokens from TWO issuers. The "iss" claim of the token selects the
 * decoder (and with it the key and the way to read the roles):
 * <ul>
 *   <li>the kingdom itself: HS256 with a shared secret, roles in the claim "roles"</li>
 *   <li>the identity provider: RS256, the public keys are fetched from its JWK set, roles come from the claim
 *       "groups" through {@link SsoRoleMapper}</li>
 * </ul>
 * With only the identity provider you would not need JwtEncoder, the secret and AuthController.login at all.
 * An unknown issuer is never trusted: the map is the allow-list.
 */
public final class MultiIssuerAuthentication {

    private MultiIssuerAuthentication() {
    }

    public static AuthenticationManagerResolver<HttpServletRequest> resolver(
            JwtDecoder kingdomDecoder, JwtAuthenticationConverter kingdomConverter,
            String idpIssuer, JwtDecoder idpDecoder) {
        Map<String, AuthenticationManager> trusted = Map.of(
                JwtService.ISSUER, manager(kingdomDecoder, kingdomConverter),
                idpIssuer, manager(idpDecoder, MultiIssuerAuthentication::fromGroups));
        return new JwtIssuerAuthenticationManagerResolver(trusted::get);
    }

    /** Validates signature and expiry with the public keys of the IdP, and that the issuer really is the IdP. */
    public static JwtDecoder idpDecoder(String idpIssuer, String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(idpIssuer));
        return decoder;
    }

    private static AuthenticationManager manager(JwtDecoder decoder,
                                                 org.springframework.core.convert.converter.Converter<Jwt, ? extends org.springframework.security.authentication.AbstractAuthenticationToken> converter) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(converter);
        return new ProviderManager(provider);
    }

    private static JwtAuthenticationToken fromGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        List<GrantedAuthority> authorities = SsoRoleMapper.rolesFor(groups == null ? List.of() : groups).stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        String name = jwt.getClaimAsString("preferred_username") != null ? jwt.getClaimAsString("preferred_username") : jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, name);
    }
}
