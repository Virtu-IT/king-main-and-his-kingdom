package com.bnp.kingmainandhiskingdom.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Exercise B4: refresh tokens with rotation. A refresh token can be used exactly once: it is exchanged for a new
 * access token AND a new refresh token, and the old one is deleted. Someone who steals an old refresh token
 * and uses it after the owner did gets a 401 (and the owner notices that they are logged out).
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final AccountService accounts;
    private final KingdomSecurityProperties properties;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository tokens, AccountService accounts,
                               KingdomSecurityProperties properties, Clock clock) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.properties = properties;
        this.clock = clock;
    }

    /** Creates a refresh token for a user and returns the token itself (the only moment it exists in clear). */
    @Transactional
    public String issue(String username) {
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        tokens.save(new RefreshToken(hash(token), username, clock.instant().plus(properties.refreshTtl())));
        return token;
    }

    /**
     * Exchanges a valid refresh token for the user it belongs to; the token is deleted (it cannot be used twice).
     * The caller issues the new tokens. Roles are read from the database again at that moment, so a role change
     * takes effect at the next refresh.
     *
     * noRollbackFor: by default a RuntimeException rolls the transaction back, and then the delete of an expired
     * token would be undone as well.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AppUser consume(String token) {
        RefreshToken stored = tokens.findByTokenHash(hash(token))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        tokens.delete(stored);
        if (!stored.getExpiresAt().isAfter(clock.instant())) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        try {
            return accounts.find(stored.getUsername());
        } catch (RuntimeException e) { // the user was deleted in the meantime
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    /** Logout: the refresh token stops working. (The short-lived access token expires by itself.) */
    @Transactional
    public void revoke(String token) {
        tokens.findByTokenHash(hash(token)).ifPresent(tokens::delete);
    }

    /**
     * Every refresh token of a user stops working (after a password change, or when an account is blocked).
     * Access tokens that were already issued stay valid until they expire: that is why they are short-lived.
     */
    @Transactional
    public void revokeAll(String username) {
        tokens.deleteByUsername(username);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
