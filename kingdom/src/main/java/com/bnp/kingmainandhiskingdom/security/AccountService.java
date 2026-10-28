package com.bnp.kingmainandhiskingdom.security;

import com.bnp.kingmainandhiskingdom.exception.AlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AccountService {

    public static final String ROLE_KING = "KING";
    public static final String ROLE_KNIGHT = "KNIGHT";
    public static final String ROLE_SUBJECT = "SUBJECT";

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

    @Transactional
    public AppUser create(String username, String rawPassword, Set<String> roles) {
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new AlreadyExistsException("User '" + username + "' already exists");
        }
        return users.save(new AppUser(username, passwordEncoder.encode(rawPassword), roles));
    }
}
