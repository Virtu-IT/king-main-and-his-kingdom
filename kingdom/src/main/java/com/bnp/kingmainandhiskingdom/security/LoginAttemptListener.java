package com.bnp.kingmainandhiskingdom.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/** Exercise B3: Spring Security publishes an event for every authentication; we only count password logins. */
@Component
public class LoginAttemptListener {

    private final LoginAttemptService loginAttempts;

    public LoginAttemptListener(LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        loginAttempts.recordFailure(event.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        // a successful JWT validation also publishes this event: only a password login resets the counter
        if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
            loginAttempts.recordSuccess(event.getAuthentication().getName());
        }
    }
}
