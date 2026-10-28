package com.bnp.kingmainandhiskingdom.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Exercise B6: the "classic" way, next to the stateless API: a login form, a session cookie (JSESSIONID) and
 * CSRF protection (switched ON, which is the default). Only for /legacy/**; the filter chains are tried in
 * order and the first one whose securityMatcher fits handles the request. The order is lower than that of the
 * API chain (2), which matches every request and must therefore come last.
 */
@Configuration
public class LegacySecurityConfig {

    @Bean
    @Order(-1)
    SecurityFilterChain legacyFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/legacy/**")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/legacy/login").permitAll()
                        .anyRequest().authenticated())
                // sessions (IF_REQUIRED) and CSRF protection are the defaults: we simply do not switch them off
                .formLogin(f -> f
                        .loginPage("/legacy/login")
                        .loginProcessingUrl("/legacy/login")
                        .defaultSuccessUrl("/legacy/hello", true))
                .logout(l -> l.logoutUrl("/legacy/logout").logoutSuccessUrl("/legacy/login?logout"));
        return http.build();
    }
}
