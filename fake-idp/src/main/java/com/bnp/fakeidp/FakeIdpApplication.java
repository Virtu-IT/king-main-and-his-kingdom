package com.bnp.fakeidp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A FAKE identity provider (OpenID Connect) for the Spring Security course, standing in for the bank's real one
 * (Entra ID, Keycloak, ...). It is a separate Spring Boot application (module fake-idp): start it next to the
 * kingdom and it listens on http://localhost:9000. Demo only, never use in production.
 */
@SpringBootApplication
public class FakeIdpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FakeIdpApplication.class, args);
    }
}
