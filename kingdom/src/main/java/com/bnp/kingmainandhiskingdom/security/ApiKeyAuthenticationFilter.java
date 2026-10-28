package com.bnp.kingmainandhiskingdom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Exercise B1: authentication with an API key for machine-to-machine calls on /api/integration/**.
 * Without the header the request simply continues (and is rejected later as "not logged in"); with a wrong
 * key it is rejected right here with a 401; with the right key the caller becomes the user "integration"
 * with the role INTEGRATION.
 *
 * Not a @Bean on purpose: Spring Boot would register every Filter bean for all requests, outside the
 * security filter chain. SecurityConfig creates it and puts it in the chain.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "api-key";
    public static final String PATH_PREFIX = "/api/integration";

    private final byte[] expectedKey;
    private final AuthenticationEntryPoint entryPoint;

    public ApiKeyAuthenticationFilter(String expectedKey, AuthenticationEntryPoint entryPoint) {
        this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
        this.entryPoint = entryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (key == null) {
            chain.doFilter(request, response);
            return;
        }
        // constant-time comparison: a normal equals() stops at the first difference and leaks how many characters were right
        if (!MessageDigest.isEqual(expectedKey, key.getBytes(StandardCharsets.UTF_8))) {
            entryPoint.commence(request, response, new BadCredentialsException("Invalid API key"));
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "integration", null, AuthorityUtils.createAuthorityList("ROLE_INTEGRATION")));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
