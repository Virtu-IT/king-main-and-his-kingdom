package com.bnp.fakeidp;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

/**
 * The whole fake identity provider in one class: OAuth2 / OpenID Connect endpoints, one registered client
 * (the kingdom), three users with groups and a fresh signing key at every start.
 * Discovery document: http://localhost:9000/.well-known/openid-configuration
 */
@Configuration
public class FakeIdpConfig {

    public static final String ISSUER = "http://localhost:9000";

    /** The OAuth2 / OIDC endpoints (/oauth2/authorize, /oauth2/token, /oauth2/jwks, /userinfo, ...). */
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        http
                .oauth2AuthorizationServer(authorizationServer -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer.oidc(Customizer.withDefaults());
                })
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                // a browser that is not logged in at the IdP goes to the login page
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    /** The login page of the identity provider. */
    @Bean
    @Order(2)
    SecurityFilterChain loginFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    /**
     * The people of the identity provider. Their GROUP_ authorities become the "groups" claim in the tokens
     * (see tokenCustomizer). The kingdom decides itself what a group means. Fake passwords: demo only.
     */
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("alice").password("{noop}alice123").authorities("GROUP_royal-court").build(),
                User.withUsername("bob").password("{noop}bob123").authorities("GROUP_guards").build(),
                User.withUsername("carol").password("{noop}carol123").authorities("GROUP_villagers").build());
    }

    /** The one application that may use this IdP: the kingdom. */
    @Bean
    RegisteredClientRepository registeredClientRepository() {
        RegisteredClient kingdom = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("kingdom-app")
                .clientSecret("{noop}kingdom-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/login/oauth2/code/kingdom-idp")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(false)
                        .requireAuthorizationConsent(false)
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(kingdom);
    }

    /** Puts "groups" and "preferred_username" in the ID token and the access token. */
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            String tokenType = context.getTokenType().getValue();
            if (!"id_token".equals(tokenType) && !"access_token".equals(tokenType)) {
                return;
            }
            List<String> groups = context.getPrincipal().getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("GROUP_"))
                    .map(a -> a.substring("GROUP_".length()))
                    .sorted()
                    .toList();
            context.getClaims().claim("groups", groups);
            context.getClaims().claim("preferred_username", context.getPrincipal().getName());
        };
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer(ISSUER).build();
    }

    /** A new RSA key pair at every start: tokens of an earlier run are no longer valid. */
    @Bean
    JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }
}
