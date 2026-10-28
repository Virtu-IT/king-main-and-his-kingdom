package com.bnp.kingmainandhiskingdom.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Exercise B5: an audit trail of security events: who logged in, who failed and who was refused. It logs
 * the username and what was attempted, never the password or a token (log files are copied around, and read
 * by more people than the database is).
 */
@Component
public class AuditLog {

    private static final Logger log = LoggerFactory.getLogger(AuditLog.class);

    @EventListener
    public void onLogin(AuthenticationSuccessEvent event) {
        // JWT validation publishes this event on every request: only log real password logins (login endpoint, Basic)
        if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
            log.info("AUDIT login success user={}", event.getAuthentication().getName());
        }
    }

    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("AUDIT login failure user={} reason={}", event.getAuthentication().getName(),
                event.getException().getClass().getSimpleName());
    }

    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> event) {
        log.warn("AUDIT access denied user={} action={}", event.getAuthentication().get().getName(), describe(event.getObject()));
    }

    private static String describe(Object object) {
        if (object instanceof MethodInvocation invocation) {
            return invocation.getMethod().getDeclaringClass().getSimpleName() + "." + invocation.getMethod().getName();
        }
        if (object instanceof HttpServletRequest request) { // a refused URL rule: the "object" is the request
            return request.getMethod() + " " + request.getRequestURI();
        }
        return String.valueOf(object);
    }
}
