package com.bnp.kingmainandhiskingdom.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountService accountService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, AccountService accountService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountService = accountService;
    }

    /** Checks username + password; a wrong combination ends up as 401 via the AuthenticationEntryPoint. */
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        JwtService.IssuedToken issued = jwtService.issue(authentication);
        return new TokenResponse(issued.token(), "Bearer", issued.expiresAt(), authentication.getName(), issued.roles());
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = accountService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserInfo(user.getUsername(), user.getRoles().stream().sorted().toList()));
    }

    /** Who am I? Works with a Bearer token and with HTTP Basic. */
    @GetMapping("/me")
    public UserInfo me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")) // Spring Security 7 also adds authorities like FACTOR_PASSWORD
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList();
        return new UserInfo(authentication.getName(), roles);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30) String username,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record TokenResponse(String token, String tokenType, Instant expiresAt, String username, List<String> roles) {
    }

    public record UserInfo(String username, List<String> roles) {
    }
}
