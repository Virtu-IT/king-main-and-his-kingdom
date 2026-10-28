package com.bnp.kingmainandhiskingdom.security;

import com.bnp.kingmainandhiskingdom.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Exercise B4. Only the SHA-256 hash of the refresh token is stored, never the token itself: whoever reads the
 * database (or a backup) cannot use the hashes to get new access tokens.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant expiresAt;

    public RefreshToken(String tokenHash, String username, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.username = username;
        this.expiresAt = expiresAt;
    }
}
