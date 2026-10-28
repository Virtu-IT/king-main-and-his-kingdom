package com.bnp.kingmainandhiskingdom.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;
import java.util.Set;

/**
 * "Log in with the identity provider" (OpenID Connect, authorization code flow). Only active with the
 * profile "idp" and with the fake IdP running on port 9000 (main class com.bnp.fakeidp.FakeIdpApplication).
 *
 * Flow: /oauth2/authorization/kingdom-idp  ->  login at the IdP  ->  /login/oauth2/code/kingdom-idp (code
 * for tokens, ID token validated with the keys of the IdP)  ->  we map the groups to our roles and issue
 * our own JWT  ->  /sso#token=... (the frontend picks it up).
 */
@Configuration
@Profile("idp")
public class SsoSecurityConfig {

    /** Own filter chain: this login needs a session (to remember state and nonce), the REST API does not. */
    @Bean
    @Order(0)
    SecurityFilterChain ssoFilterChain(HttpSecurity http, AuthenticationSuccessHandler ssoSuccessHandler) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .oauth2Login(login -> login.successHandler(ssoSuccessHandler));
        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler ssoSuccessHandler(JwtService jwtService) {
        return (request, response, authentication) -> {
            OidcUser user = (OidcUser) authentication.getPrincipal();
            List<String> groups = user.getClaimAsStringList("groups");
            Set<String> roles = SsoRoleMapper.rolesFor(groups == null ? List.of() : groups);
            String username = user.getPreferredUsername() != null ? user.getPreferredUsername() : user.getSubject();
            JwtService.IssuedToken token = jwtService.issue(username, roles);
            // the session at this point is only needed for the login itself
            request.getSession().invalidate();
            response.sendRedirect("/sso#token=" + token.token());
        };
    }
}
