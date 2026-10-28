package com.bnp.kingmainandhiskingdom.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Day 2: the user store is the database instead of InMemoryUserDetailsManager. */
@Service
public class JpaUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final AppUserRepository users;

    public JpaUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user '" + username + "'"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRoles().toArray(String[]::new))
                .build();
    }

    /**
     * A7: called by Spring Security after a successful login when the stored hash uses an older algorithm
     * (PasswordEncoder.upgradeEncoding): the new hash replaces the old one.
     */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        AppUser appUser = users.findByUsernameIgnoreCase(user.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user '" + user.getUsername() + "'"));
        appUser.setPasswordHash(newPassword);
        return User.withUserDetails(user).password(newPassword).build();
    }
}
