package com.bnp.kingmainandhiskingdom.security;

import com.bnp.kingmainandhiskingdom.exception.AlreadyExistsException;
import com.bnp.kingmainandhiskingdom.exception.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AccountService {

    public static final String ROLE_KING = "KING";
    public static final String ROLE_KNIGHT = "KNIGHT";
    public static final String ROLE_SUBJECT = "SUBJECT";
    public static final String ROLE_STEWARD = "STEWARD";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Self-registration: you always become a plain SUBJECT; only the king's database admin can promote you. */
    @Transactional
    public AppUser register(String username, String rawPassword) {
        return create(username, rawPassword, Set.of(ROLE_SUBJECT));
    }

    public static final Set<String> KNOWN_ROLES = Set.of(ROLE_KING, ROLE_KNIGHT, ROLE_SUBJECT, ROLE_STEWARD);

    /**
     * B2: replaces the roles of a user. Only known roles; the king cannot take their own KING role away
     * (otherwise nobody could manage the roles anymore).
     */
    @Transactional
    public AppUser changeRoles(String callerUsername, String username, java.util.Collection<String> roles) {
        for (String role : roles) {
            if (!KNOWN_ROLES.contains(role)) {
                throw new IllegalArgumentException("Unknown role '" + role + "'");
            }
        }
        if (callerUsername.equalsIgnoreCase(username) && !roles.contains(ROLE_KING)) {
            throw new IllegalArgumentException("You cannot remove your own KING role");
        }
        AppUser user = find(username);
        user.setRoles(new java.util.HashSet<>(roles));
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser find(String username) {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User '" + username + "' could not be found"));
    }

    /** A wrong old password is a 400 (not a 401: the caller is logged in, and a 401 would log the frontend out). */
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        AppUser user = find(username);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("The old password is not correct");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public AppUser create(String username, String rawPassword, Set<String> roles) {
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new AlreadyExistsException("User '" + username + "' already exists");
        }
        return users.save(new AppUser(username, passwordEncoder.encode(rawPassword), roles));
    }
}
