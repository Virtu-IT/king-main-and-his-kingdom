package com.bnp.kingmainandhiskingdom.security;

import com.bnp.kingmainandhiskingdom.exception.HttpCatImages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * The security setup of the kingdom, the heart of the Spring Security course.
 *
 * <ul>
 *   <li>Day 1: filter chain, authorizeHttpRequests (URL rules), roles, 401 vs 403, HTTP Basic,
 *       PasswordEncoder, CORS, CSRF, method security (@PreAuthorize on the controllers)</li>
 *   <li>Day 2: users in the database (JpaUserDetailsService), login that returns a JWT,
 *       stateless resource server that validates the token (Bearer), testing with MockMvc</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(KingdomSecurityProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * A second filter chain just for the H2 console (only with the "local" profile): it is a page with
     * frames and its own forms, so it needs other rules than the REST API. The first matching chain wins.
     */
    @Bean
    @Order(1)
    @Profile("local")
    SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/h2-console/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http,
                                       JwtAuthenticationConverter jwtAuthenticationConverter,
                                       AuthenticationEntryPoint entryPoint,
                                       AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // A REST API with tokens in the Authorization header has no session cookie that a
                // malicious site could ride on, so CSRF protection is switched off. Never do this for
                // an application that logs in with cookies!
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        // public: login, registration, a welcome message, health and the API documentation
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/welcome").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // the rest of the actuator is for the king only
                        .requestMatchers("/actuator/**").hasRole("KING")
                        // A2: statistics for the king and the knights only (URL rule)
                        .requestMatchers("/api/stats").hasAnyRole("KING", "KNIGHT")
                        // A3: a steward may not see the persons (the order matters: the first match wins)
                        .requestMatchers(HttpMethod.GET, "/api/persons/**").hasAnyRole("KING", "KNIGHT", "SUBJECT")
                        // every other API call needs a login; finer rules are @PreAuthorize on the controllers
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // everything else is the Angular frontend (static files and client-side routes)
                        .anyRequest().permitAll())
                // HTTP Basic: handy for curl and Swagger UI; credentials in every request
                .httpBasic(b -> b.authenticationEntryPoint(entryPoint))
                // Bearer tokens: the frontend logs in once and sends the JWT afterwards
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /** 401: you are not logged in (or the credentials / token are wrong). */
    @Bean
    AuthenticationEntryPoint restAuthenticationEntryPoint(HttpCatImages images) {
        return (request, response, exception) -> {
            // The browser's own login popup would appear on a 401 with this header. Our own frontend
            // marks its calls with X-Requested-With so that it can show its login page instead.
            if (request.getHeader("X-Requested-With") == null) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"kingdom\"");
            }
            write(response, images.response(HttpStatus.UNAUTHORIZED, "Authentication required: " + exception.getMessage()));
        };
    }

    /** 403: you are logged in, but your role is not allowed to do this. */
    @Bean
    AccessDeniedHandler restAccessDeniedHandler(HttpCatImages images) {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.access.AccessDeniedException exception) ->
                write(response, images.response(HttpStatus.FORBIDDEN, "You do not have the role for this action"));
    }

    private static void write(HttpServletResponse response, ResponseEntity<byte[]> body) throws IOException {
        response.setStatus(body.getStatusCode().value());
        body.getHeaders().forEach((name, values) -> values.forEach(v -> response.addHeader(name, v)));
        response.getOutputStream().write(body.getBody());
    }

    /**
     * CORS: which other origins may call this API from a browser. The Angular dev server (port 4200)
     * is another origin than Spring Boot (port 8080). The packaged frontend is served by Spring Boot
     * itself, so it does not need CORS at all.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(KingdomSecurityProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.corsAllowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        cors.setExposedHeaders(List.of(HttpCatImages.ERROR_MESSAGE_HEADER));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
