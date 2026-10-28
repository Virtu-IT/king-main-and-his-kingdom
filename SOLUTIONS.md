# Solutions

This is the `solution` branch: the complete, working solution of every exercise in [EXERCISES.md](EXERCISES.md), with the
answer to every question. The `security` branch is the starting point for the participants (the same project without
the solutions). Everything here is built, tested (`mvn test`: 161 tests in `kingdom`, 2 in `fake-idp`) and, where it
matters, tried out against running applications. Paths are relative to the `kingdom` module
(`src/main/java/com/bnp/kingmainandhiskingdom/...`) unless stated otherwise.

The exercises overlap, so some answers build on each other. Do not hand this file out before the exercises are done.

## What surprised us while building the solutions

These are real traps we ran into. They make good discussion points.

1. **Spring Security 7 adds authorities of its own** after a login: `FACTOR_PASSWORD` for a password and `FACTOR_BEARER`
   for a bearer token. Code that treats every authority as a role shows them in `/me` and in the token. `AuthController`
   and `JwtService` only keep the authorities that start with `ROLE_`.
2. **`requiresChannel()` is gone at runtime.** The method still compiles, but `new`-ing the filter chain fails with
   `NoClassDefFoundError: ...ChannelDecisionManager` (Spring Security 7.1). Redirecting HTTP to HTTPS is done by the proxy
   or by a second Tomcat connector, not by `HttpSecurity` anymore (see B7).
3. **A `RuntimeException` rolls the transaction back, also the delete you just did.** In `RefreshTokenService.consume`
   an expired token has to be deleted *and* refused with an exception: `@Transactional(noRollbackFor = BadCredentialsException.class)`.
4. **The frontend fallback swallowed `/actuator/env`**: our `AngularFrontendConfig` answered every URL without a dot and
   without `api/` with `index.html` (200). A king got `200 <html>` instead of a 404. Server URLs are never client-side routes.
5. **A `Filter` that is a `@Bean` is registered by Spring Boot for every request**, outside the security filter chain. The
   `ApiKeyAuthenticationFilter` is created with `new` in `SecurityConfig` for that reason.
6. **Cookies are not separated by port.** The kingdom (8080) and the fake IdP (9000) on `localhost` both used `JSESSIONID`
   and logged each other out. The IdP has its own cookie name (`FAKEIDP_SESSION`).
7. **A "tampered token" test that changes the last character can pass.** The last character of a base64url signature can
   carry unused bits, so flipping it may not change the decoded signature. Change the payload instead.
8. **`AuthenticationSuccessEvent` fires on every request with a JWT**, not only at a login. The audit log and the lock
   counter only look at `UsernamePasswordAuthenticationToken`.
9. **`@Column(nullable = false)` and primitive types become `NOT NULL` in a SINGLE_TABLE subclass too**, which makes
   rows of the other subclasses impossible (C2).
10. **The object of an `AuthorizationDeniedEvent` depends on the kind of rule:** a `MethodInvocation` for `@PreAuthorize`,
    the `HttpServletRequest` for a URL rule (B5).

---

## Part A: Spring Security, day 1

### A1 Explore the security

**1. Status codes of `GET /api/persons`**

| Caller | Status | Why |
|---|---|---|
| anonymous | **401** | `/api/**` needs a login. The answer carries `WWW-Authenticate: Basic realm="kingdom"` and `X-Error-Message: Authentication required`. |
| `peasant` (SUBJECT) | 200 | logged in, and `SUBJECT` may read persons |
| `king` | 200 | |

**2. `DELETE /api/persons/Merlin`:** `peasant` 403, `lancelot` 403, `king` 204.
`peasant` and `lancelot` *are* logged in, so the URL rule (`/api/**` is `authenticated()`) lets them through. Then the
method security interceptor evaluates `@PreAuthorize("hasRole('KING')")` on `PersonRestController.deletePerson`.
`SUBJECT` and `KNIGHT` are not `KING`, which throws an `AccessDeniedException`; it ends in `restAccessDeniedHandler`
(403). Lancelot is a knight, not a king: roles are not hierarchical here. The king may delete, and Merlin exists: 204.
(Restart the application to bring Merlin back.)

**3. Swagger UI:** `POST /api/auth/login` with `{"username":"king","password":"..."}`, copy the `token` from the answer,
press *Authorize*, paste it in `bearerAuth` (without the word "Bearer"). Swagger sends `Authorization: Bearer <token>`
from now on. (The second scheme, `basicAuth`, sends username and password in every request.)

**4. Predictions for the Security lab**

| Call | anonymous | peasant (SUBJECT) | lancelot (KNIGHT) | king | steward (solution branch only) |
|---|---|---|---|---|---|
| Welcome (`/api/welcome`) | 200 | 200 | 200 | 200 | 200 |
| Who am I? | 401 | 200 | 200 | 200 | 200 |
| List persons | 401 | 200 | 200 | 200 | 403 (A3) |
| Add equipment | 401 | 403 | 201 | 201 | 201 (A3) |
| Delete a person that does not exist | 401 | 403 | 403 | **404** (allowed, but nobody to delete) | 403 |
| Actuator environment | 401 | 403 | 403 | **404** (allowed by the URL rule, but the endpoint is not exposed) | 403 |
| Actuator health | 200 | 200 | 200 | 200 | 200 |

The 404 for the king is the trick: authorization comes *before* "does it exist". An unauthorized caller learns nothing
about what exists. (For `/actuator/env` that only became true after fix 4 in the list above.)

**5. `kingdom.security.jwt-ttl: 30s`:** after 30 seconds the access token is expired. The next API call is answered with
`401` (the `BearerTokenAuthenticationFilter` fails, `restAuthenticationEntryPoint` answers). The frontend interceptor
sees a 401 *while it had sent a token*, calls `auth.logout()` and shows the login page. A token is not extended by use
(stateless: the server remembers nothing), so the user has to log in again. B4 (refresh tokens) is the answer to this.

**Where the 401 and the 403 are made**
- **401:** `SecurityConfig.restAuthenticationEntryPoint` (an `AuthenticationEntryPoint`). It is called by the
  `ExceptionTranslationFilter` when someone who is *not authenticated* is refused, and by the Basic and Bearer filters when
  the credentials or the token are wrong.
- **403:** `SecurityConfig.restAccessDeniedHandler` (an `AccessDeniedHandler`), called by the same filter when someone
  who *is* authenticated lacks the authority. The `AccessDeniedException` is thrown by the `AuthorizationFilter` (URL
  rules) or by the method interceptor (`@PreAuthorize`).

401 means "I do not know who you are", 403 means "I know who you are, and you may not".

### A2 A new endpoint with a URL rule

Code: `controller/StatsRestController`, `services/KingdomStatsService`, the rule in `SecurityConfig`. Tests:
`StatsRestControllerTest`.

1. Without a specific rule, `/api/**` is `authenticated()`: everyone who is logged in may see it (the test with
   `SUBJECT` would give 200 before step 2).
2. `.requestMatchers("/api/stats").hasAnyRole("KING", "KNIGHT")`, placed **before** `/api/**`. The first matching rule wins,
   so a more general rule above it would make this one dead.
3. The alternative is `@PreAuthorize("hasAnyRole('KING', 'KNIGHT')")` on `StatsRestController.stats()`.

**When which?**
- **URL rule** (in `SecurityConfig`): one place that shows the whole access policy; it also protects URLs that are not
  controllers (static files, actuator, Swagger); it is evaluated early, before Spring MVC and before the body is read.
  Coarse: it only knows the URL and the HTTP method.
- **Method security**: next to the code it protects; it can use the arguments and the result (`#username`,
  `returnObject`), so it is the place for ownership and business rules; it also protects the service layer when called
  from other entry points (scheduler, messaging). It needs a proxy: a call from another method of the same class
  (self-invocation) bypasses it, and it does not work on private methods.
- A good rule: URL rules for broad areas (public, logged in, admin), method security for the fine rules, and both for
  sensitive operations (defence in depth).

### A3 A new role

Code: `AccountService.ROLE_STEWARD`, the user `steward` in `config/DataInitializer`, in `SecurityConfig`
`.requestMatchers(HttpMethod.GET, "/api/persons/**", "/api/knights/**").hasAnyRole("KING", "KNIGHT", "SUBJECT")`,
and `@PreAuthorize("hasAnyRole('KING', 'KNIGHT', 'STEWARD')")` on `ArsenalRestController.addEquipment`. Test: `StewardRoleTest`.

1. The steward is created through `AccountService.create`, like the other users (the password is hashed there).
2. `GET /api/persons` now lists the roles that are allowed: `KING`, `KNIGHT`, `SUBJECT`. The rule is for `GET` only:
   writing is already restricted by `@PreAuthorize`. Without the extra `/api/knights/**` the steward could still read
   the names of all knights there, a hole you only find by testing.
3. **The frontend shows the *Persons* link to a steward. Is that a problem?** No, as long as the *server* refuses: a
   steward who clicks the link gets a 403. The browser is not trusted: anyone can call the API directly with `curl`,
   change the JavaScript, or edit the stored session. Hiding a button is user experience, not security. The same
   holds for the Angular `authGuard`. An authorization rule that exists only in the user interface does not exist.

### A4 Ownership check with SpEL

Code: `security/UserController`, `AccountService.find` and `changePassword`. Tests: `UserControllerTest`.

- `@PreAuthorize("#username.equalsIgnoreCase(authentication.name) or hasRole('KING')")`: `#username` is the method
  parameter (the path variable; this needs the `-parameters` compiler flag, which the Spring Boot parent enables);
  `authentication` is the current `Authentication`.
- The password change is `@PreAuthorize("#username.equalsIgnoreCase(authentication.name)")`: only the owner, not even
  the king. The old password is checked with `passwordEncoder.matches`; a wrong one is a **400** (not 401: the caller *is*
  logged in, and a 401 would make the frontend log them out). The new password gets the same validation as at
  registering (`@Size(min = 8, max = 72)`; 72 bytes is the maximum that BCrypt looks at).
- The hash is replaced inside a `@Transactional` method on a managed entity: no `save()` needed (see C6).

**Do existing tokens stay valid after a password change? Is that bad?** Yes, they stay valid: a JWT is self-contained
and the server keeps no session. That is why an access token is short-lived (5 minutes after B4), and why the solution
also deletes **all refresh tokens** of the user when the password changes (`RefreshTokenService.revokeAll`): a thief who
copied a refresh token loses it as soon as the owner changes the password. The remaining window is the lifetime of the
access token. With HTTP Basic there is no such window: the password is checked against the database on every request.

### A5 Test the security

`security/ArsenalSecurityMatrixTest`: a `@ParameterizedTest` with a `@MethodSource` of 16 combinations (4 endpoints
x anonymous, SUBJECT, KNIGHT, KING). `@WithMockUser` cannot be parameterized (an annotation takes constants), so the user is
added per call with `.with(user("x").roles(role))` of `SecurityMockMvcRequestPostProcessors`.

| | anonymous | SUBJECT | KNIGHT | KING |
|---|---|---|---|---|
| `GET /api/equipment` | 401 | 200 | 200 | 200 |
| `POST /api/equipment` | 401 | 403 | 201 | 201 |
| `GET /api/war-machines` | 401 | 200 | 200 | 200 |
| `POST /api/war-machines` | 401 | 403 | 403 | 201 |

(`STEWARD` may add equipment since A3; it is covered by `StewardRoleTest`.)

### A6 Investigate CORS

Tried for real: `jwebserver -p 4200 -d kingdom/exercises` and the kingdom on 8080.

1. **`http://localhost:4200/cors-test.html`:** `GET /api/welcome` gives `200 Welcome to the Kingdom, Guest!`; `GET /api/persons`
   gives `401` (without token). The page can *read* the 401, because the answer carries `Access-Control-Allow-Origin`.
2. **`http://127.0.0.1:4200/cors-test.html`:** both calls fail with "Failed to fetch"; the console says *"has been blocked
   by CORS policy: No 'Access-Control-Allow-Origin' header is present"*. An **origin** is protocol + host + port.
   `localhost` and `127.0.0.1` are different hosts, so `http://127.0.0.1:4200` is a different origin than the configured
   `http://localhost:4200`. The server did not even answer positively: the browser blocks the page from reading the answer.
3. Adding `http://127.0.0.1:4200` to `kingdom.security.cors-allowed-origins` (and restarting) makes the second page work too.
4. **The preflight.** A request with a non-simple header (`Authorization`) makes the browser first send
   `OPTIONS /api/persons` with `Origin`, `Access-Control-Request-Method: GET` and `Access-Control-Request-Headers: authorization`.
   The answer (from `curl`, same as the browser sees):

   ```
   HTTP/1.1 200
   Access-Control-Allow-Origin: http://localhost:4200
   Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS
   Access-Control-Allow-Headers: authorization
   Access-Control-Expose-Headers: X-Error-Message
   Vary: Origin, Access-Control-Request-Method, Access-Control-Request-Headers
   ```
   A preflight from `http://127.0.0.1:4200` gets `403 Invalid CORS request` instead. A plain `GET /api/welcome` has no
   preflight (a "simple request"), but the browser still sends the `Origin` header and still checks the answer.

**Why is CORS an agreement with the browser, and not protection of the server?** CORS lets a *browser* decide whether the
JavaScript of site A may read the answer of site B (with the cookies or credentials of the user). It protects the
**user** against other sites. The server cannot enforce it: a client that is not a browser ignores the headers, or just
omits `Origin`: `curl http://localhost:8080/api/welcome` (without `Origin`) is answered normally. (Spring itself does
answer a request with a *foreign* `Origin` header with `403 Invalid CORS request`, even from curl, but an attacker simply
leaves the header out.) The real protection of the API is authentication and authorization. Also: CORS is not CSRF
protection, and `Access-Control-Allow-Origin: *` together with credentials is never a good idea.

### A7 Another password algorithm

Code: `SecurityConfig.passwordEncoder` (a `DelegatingPasswordEncoder` with `{pbkdf2}` as the default and `{bcrypt}`
still supported), `JpaUserDetailsService.updatePassword`. Tests: `PasswordUpgradeTest`, `SecurityTest`.

1. With `createDelegatingPasswordEncoder()` the column `password_hash` contains the id of the algorithm in braces in
   front of the hash: `{bcrypt}$2a$10$...`. In our solution new hashes start with `{pbkdf2}`. The prefix is what
   makes it possible to change the algorithm without invalidating existing passwords: the encoder looks at the prefix
   to know how to check.
2. Our own `DelegatingPasswordEncoder("pbkdf2", Map.of("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(), "bcrypt", new BCryptPasswordEncoder()))`.
   `Pbkdf2PasswordEncoder` is part of `spring-security-crypto`: no extra dependency (Argon2 and SCrypt need Bouncy Castle).
3. `JpaUserDetailsService` also implements `UserDetailsPasswordService`. After a *successful* login Spring Security asks
   `passwordEncoder.upgradeEncoding(hash)`; for an old `{bcrypt}` hash that is true, so `updatePassword` is called with
   the new `{pbkdf2}` hash (the plain password is only known at that moment). A wrong password never upgrades anything
   (`PasswordUpgradeTest`).

**Why a slow hash with a salt, and why never MD5 or SHA-1?**
- A password hash must be **slow**: a thief with the database tries billions of guesses. MD5 and SHA-1 were designed to
  be fast (billions per second on one GPU); PBKDF2 (hundreds of thousands of iterations), bcrypt, scrypt and Argon2
  make every guess expensive and can be made more expensive over time (the *work factor*, and that is what
  `upgradeEncoding` is for).
- A **salt** is a random value per password, stored with the hash. Two users with the same password get different hashes,
  and precomputed tables (rainbow tables) become useless.
- MD5 and SHA-1 also have collision attacks, and no salt: they are not meant for passwords at all.

---

## Part B: Spring Security, day 2

### B1 API key for an integration

Code: `security/ApiKeyAuthenticationFilter`, `controller/IntegrationRestController`, `kingdom.security.api-key` in
`KingdomSecurityProperties` (override with the environment variable `KINGDOM_SECURITY_API_KEY`), the rules in
`SecurityConfig` (`/api/integration/**` needs `ROLE_INTEGRATION`, the filter is added with `addFilterBefore(...,
BearerTokenAuthenticationFilter.class)`). Tests: `ApiKeyTest`.

| Request | Result |
|---|---|
| right key in `api-key` | 200 |
| no key | 401 (the filter lets the request through; the URL rule then finds nobody logged in) |
| wrong or empty key | 401 (the filter calls `restAuthenticationEntryPoint` itself) |
| a normal user (king, with a login) without `api-key` | **403** (logged in, but not `INTEGRATION`) |
| the right key on another URL (`/api/persons`) | 401 (the filter only works under `/api/integration`) |

**Why `MessageDigest.isEqual` instead of `equals`?** `String.equals` stops at the first character that differs, so the
time of the answer reveals how many leading characters were right: a *timing attack* lets an attacker find the key
character by character. `MessageDigest.isEqual` takes the same time however many characters match. (Also in a real
system: a key per client, stored hashed, rotated, never in a URL because URLs end up in logs.)

### B2 The king manages roles

Code: `security/AdminRestController` (`PUT /api/admin/users/{username}/roles`; protected twice: a URL rule for
`/api/admin/**` and `@PreAuthorize`), `AccountService.changeRoles` (`@Transactional`, `KNOWN_ROLES`). Tests:
`RoleAdministrationTest`.

- Unknown roles give 400 (`"EMPEROR"`), an empty list gives 400 (`@NotEmpty`), an unknown user gives 404, a knight gets 403.
- The king cannot remove their own `KING` role: otherwise the last administrator can lock everybody out by accident.

**Does `peasant` get the new rights immediately with the existing token? Why not?** No. The roles are a *claim in the
JWT*, written when the token was issued. A resource server only checks the signature and the expiry; it does not look at
the database. (With HTTP Basic the user is read from the database on every request, so the change is effective
immediately. That is the difference, and why the exercise says "token".)

**How to solve it?**
1. **Short access tokens + refresh tokens** (B4): the roles are read again from the database at every refresh
   (`RefreshTokenService.consume`; `RefreshTokenTest.aRefreshReadsTheRolesFromTheDatabaseAgain` proves it). The delay is
   at most the lifetime of an access token. This is the usual answer.
2. **Load the roles from the database on every request** (a `JwtAuthenticationConverter` that queries): immediate, but it
   removes the benefit of stateless tokens.
3. **A revocation list or a token version** (a `token_version` claim that is compared with the database): precise, but
   stateful again. Needed when a demotion must be effective right away.

### B3 Account lock after failed logins

Code: `security/LoginAttemptService` (in memory, a `Clock` is injected: `config/ClockConfig`), `LoginAttemptListener`
(`@EventListener` on `AuthenticationFailureBadCredentialsEvent` and `AuthenticationSuccessEvent`),
`AuthController.login` (asks first), `JpaUserDetailsService` (`accountLocked(...)`, so HTTP Basic cannot get around the
lock), `exception/TooManyAttemptsException` and its handler (429 + `Retry-After`). Tests: `LoginLockTest` with a
`MutableClock`.

Settings: `kingdom.security.max-login-attempts` (3) and `kingdom.security.login-lock-duration` (1 minute).
Behaviour (tested): three wrong logins give 401, the fourth attempt, **with the correct password**, gives `429` with
`Retry-After: 60`; after 30 seconds it is `Retry-After: 30`; after 61 seconds the login works again. A successful login
resets the counter. An unknown user is counted and blocked in exactly the same way.

**How would you do it in a cluster?** The counter must live in a place that all instances share: a database table, or
Redis with an expiring key. Or rate limit before the application (reverse proxy, API gateway). With an in-memory map each
instance has its own counter, so an attacker gets the limit once per instance.

**Things to discuss**
- **User enumeration:** the answer must not reveal which accounts exist. That is why an unknown username gets the same
  429, and why `restAuthenticationEntryPoint` now always says `Authentication required` (the exception message "User
  account is locked" would have revealed that the account exists).
- **Lockout is a denial-of-service tool:** an attacker can lock a victim out on purpose. Mitigations: lock per username
  *and* IP address, increasing delays instead of a hard lock, a CAPTCHA, a notification to the user.
- The map grows with every username that was ever tried: in production expire entries.

### B4 Refresh tokens

Code: `security/RefreshToken` (+ repository), `RefreshTokenService` (issue, `consume` = use once, `revoke`, `revokeAll`),
`AuthController` (`/api/auth/login` now also returns `refreshToken`, `/api/auth/refresh`, `/api/auth/logout`), the
settings `jwt-ttl: 5m` and `refresh-ttl: 8h`. Tests: `RefreshTokenTest`.

- A refresh token is 32 random bytes (base64url, `SecureRandom`). Only its SHA-256 hash is stored, with the user and the
  expiry (`refresh_token` table).
- `POST /api/auth/refresh` exchanges it for a new access token **and** a new refresh token (rotation); the old one is
  deleted in the same transaction, so using it a second time gives 401. The roles are read from the database again.
- `POST /api/auth/logout` deletes the refresh token. A password change deletes all of them (A4).
- An expired token is deleted and refused: `@Transactional(noRollbackFor = BadCredentialsException.class)`, otherwise the
  rollback undoes the delete (see "surprised us" 3).

**Why store a refresh token as a hash?** A refresh token is as good as a password for as long as it is valid. Whoever can
read the database, a backup or a log can otherwise use it. With only hashes stored, a leaked table gives nothing. Note
that a *fast* hash (SHA-256) is enough here, unlike for passwords: a refresh token is 256 bits of randomness, so there is
nothing to guess and a slow hash adds nothing.

**Why rotation?** If a stolen token is used by a thief *and* by the owner, one of them is the second user of an old
token and gets 401. (A more advanced setup treats the reuse of an old token as a theft and revokes the whole chain.)

Not done here (discuss): a scheduled job to delete expired tokens; keeping the refresh token in an `HttpOnly` cookie. **The
frontend does not use refresh tokens**: it stores the whole login answer, including the refresh token, in
`sessionStorage`, and it logs the user out after the 5 minutes of the access token. A real client keeps the refresh token
out of reach of JavaScript.

### B5 Audit log

Code: `security/AuditLog` (three `@EventListener`s), `SecurityConfig.authorizationEventPublisher` (a
`SpringAuthorizationEventPublisher` bean: without it Spring Security does not publish `AuthorizationDeniedEvent`).
Tests: `AuditLogTest` (with `OutputCaptureExtension`).

```
AUDIT login success user=lancelot
AUDIT login failure user=lancelot reason=BadCredentialsException
AUDIT access denied user=peasant action=PersonRestController.deletePerson
AUDIT access denied user=peasant action=GET /api/stats
```
`action` is the `MethodInvocation` for `@PreAuthorize`, and the request for a URL rule. The tests prove that the log
contains neither the password nor a wrong guess nor a token.

**Why never log a password or a token?** Log files are copied, kept for years, shipped to other systems and read by more
people than the database is. A token in a log is a session that anybody with access to the log can take over; a password
(also a wrong one, which is often a typo of the right one) is a credential. It also conflicts with privacy and
compliance rules. Log *who* and *what*, never the secret.

### B6 Sessions and CSRF

Code: `security/LegacySecurityConfig` (a second filter chain for `/legacy/**`: `formLogin`, a session, CSRF on),
`controller/LegacyController` (HTML with the `CsrfToken` in a hidden field; output is escaped with `HtmlUtils`). Tests:
`LegacyCsrfTest`. The chain has `@Order(-1)`: the h2 console chain has 1, the single sign-on chain 0 and the API chain 2,
which matches every request and must come last.

Observed (and tested): `GET /legacy/hello` without a login redirects to `/legacy/login`; a login `POST` **without** the
`_csrf` field gives **403**; with it, 302 to `/legacy/hello` and a `JSESSIONID` session cookie; a `POST /legacy/note` in a
logged-in session **without** the token also gives 403, with the token 303. A `POST /api/persons` with HTTP Basic and no
CSRF token simply works.

**Why is CSRF protection not needed for the JWT API?** In a *cookie* login the browser attaches the cookie to **every**
request to that site, also to a request that a page of `evil.example` makes (an auto-submitted form). The attacker cannot
read the answer, but the action happens, with your rights. The CSRF token is a secret that the attacker's page cannot
know (the same-origin policy keeps it from reading ours) and that has to be in the request. With the JWT API the
credential is in the `Authorization` header: the browser does **not** attach it by itself. JavaScript has to add it, and
the script of another site does not have our token. Nothing is sent automatically, so there is nothing to forge.

```
cookie login:   browser --(cookie is attached automatically)--> bank.example   <-- evil.example triggers the request
header login:   browser --(only our own JavaScript adds Authorization: Bearer ...)--> API   <-- evil.example has no token
```

**What can go wrong if you keep a JWT in a cookie?** Two things, opposite to each other:
- A cookie is sent automatically, so **CSRF is back** (you need CSRF protection or `SameSite=Strict/Lax` plus a token).
- An `HttpOnly` cookie cannot be read by JavaScript, which is *better* against **XSS** than `sessionStorage` or
  `localStorage` (where one injected script can steal the token). It is a trade-off: header + storage = no CSRF but XSS
  exposure; `HttpOnly` cookie = XSS-safe token but CSRF protection needed.

### B7 HTTPS

Code: `application-https.yaml` (run with `--spring.profiles.active=local,https`; the application is then on
`https://localhost:8443`), `kingdom-dev.p12` (a self-signed development keystore in `src/main/resources`, **its private key
is in git: development only**), the HSTS header in `SecurityConfig`.

1. `keytool -genkeypair -alias kingdom -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore kingdom-dev.p12 -validity 3650 -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"` (`keytool` is part of the JDK; the
   `SAN` extension is needed: browsers ignore the CN).
2. `server.ssl.enabled=true`, `key-store`, `key-store-type`, `key-store-password` (from an environment variable, here
   with a development default), `key-alias`.
3. **The browser warns** ("Your connection is not private" / "not secure") because the certificate is signed by itself and
   not by a certificate authority that the browser trusts, so the identity of the server is not proven. The encryption
   itself works. `curl` needs `-k` (or `--cacert`) for the same reason. In production the certificate comes from a CA
   (or the bank's internal CA that the clients trust). Key store versus trust store: the *key store* holds *our own* key and
   certificate; the *trust store* holds the certificates of the parties *we* trust.
4. Checked: `https://localhost:8443/api/welcome` returns 200 with
   `Strict-Transport-Security: max-age=31536000 ; includeSubDomains`; plain `http://localhost:8443` is refused (400).

**HSTS:** the server tells the browser "for the next year, use HTTPS only for this host (and its subdomains)". That stops
*downgrade* and *SSL stripping* attacks, where an attacker on the network keeps the user on plain HTTP. The header is
only sent over HTTPS, because over HTTP an attacker could have inserted or removed it.

**How to redirect HTTP to HTTPS?** `requiresChannel().anyRequest().requiresSecure()` no longer works in this Spring
Security version (it fails at startup with a `NoClassDefFoundError`). Usually the proxy or load balancer does it (and the
application reads `X-Forwarded-Proto` with `server.forward-headers-strategy=framework`). Without a proxy: a second Tomcat
connector on port 8080 that redirects to 8443 (`TomcatServletWebServerFactory.addAdditionalTomcatConnectors`).

**Why are Basic and JWT unsafe without HTTPS?** Both are *bearer* credentials: whoever sees them can use them. Basic is
only base64 (no encryption). A bearer token that travels in clear text can be copied by anyone on the Wi-Fi, in a proxy
or on a router, and replayed until it expires. HTTPS also protects the integrity of the answers (nobody can change
them on the way) and proves the identity of the server.

### B8 OAuth 2.0 and OpenID Connect with the fake IdP

Code: module `fake-idp` (`FakeIdpConfig`), in `kingdom`: `application-idp.yaml`, `security/SsoSecurityConfig`,
`SsoRoleMapper`, `MultiIssuerAuthentication`. Tests: `SsoRoleMapperTest`, `MultiIssuerAuthenticationTest`,
`FakeIdpApplicationTest`. Everything was also tried against the running applications with a script that plays the
browser (`curl` with a cookie jar).

**1. The authorization code flow, step by step**
1. The browser goes to `http://localhost:8080/oauth2/authorization/kingdom-idp`. The kingdom (the *client*) creates a random
   `state` and a `nonce`, keeps them in the session (the `JSESSIONID` cookie) and answers **302** to the IdP:
   `http://localhost:9000/oauth2/authorize?response_type=code&client_id=kingdom-app&scope=openid profile&state=...&nonce=...&redirect_uri=http://localhost:8080/login/oauth2/code/kingdom-idp`.
2. The IdP does not know the user yet: **302** to its `/login`. The user logs in at the IdP (**the password goes to the IdP,
   never to the kingdom**), and the IdP continues with the saved `/oauth2/authorize` request.
3. The IdP answers **302** to `redirect_uri?code=...&state=...`. The `code` is short-lived and can be used once.
4. The browser follows it: `GET /login/oauth2/code/kingdom-idp?code=...&state=...`. The kingdom checks that `state` is the
   one it generated (protection against CSRF on the login).
5. **The back channel:** the kingdom (the server, *not* the browser) does `POST http://localhost:9000/oauth2/token` with the
   `code` and its own `client_id` and `client_secret` (HTTP Basic) and gets the tokens: `access_token`, `id_token`,
   `refresh_token`. The browser never sees the tokens or the secret.
6. The kingdom validates the ID token (see 4), maps the groups to roles (2), issues its own JWT and redirects to
   `/sso#token=...`, where the frontend picks it up (and removes it from the address bar).

**2. Roles per group** (`SsoRoleMapper`)

| IdP user | Group | Role(s) in the kingdom |
|---|---|---|
| alice | royal-court | KING, KNIGHT |
| bob | guards | KNIGHT |
| carol | villagers | SUBJECT |
| dave | merchants | STEWARD (task 3) |
| (unknown group) | | SUBJECT (a plain subject, never more) |

**Why does that logic live in the application and not in the IdP?** The IdP knows *who* someone is and in which *groups*
they are (an organisational fact). What a group may do *in this application* is an application matter: the same group can
mean something else in another application, and the IdP cannot know the roles of every application. Keeping the mapping
in the application also means that a change of rights does not need a change at the IdP, and that an unknown group gets
the lowest rights by default (least privilege).

**3. A new group:** `dave` with group `merchants` in `FakeIdpConfig`, and `"merchants" -> STEWARD` in `SsoRoleMapper`
(`SsoRoleMapperTest.merchantsAreStewards`). Checked live with a real access token of the IdP: `/api/auth/me` gives
`{"username":"dave","roles":["STEWARD"]}`, `GET /api/persons` 403, `GET /api/equipment` 200, `POST /api/equipment` 201.

**4. The ID token.** The real tokens of the IdP (for `dave`):

```
header:  {"kid": "<id of the signing key>", "alg": "RS256"}
id token {"sub": "dave", "aud": "kingdom-app", "azp": "kingdom-app", "iss": "http://localhost:9000",
          "groups": ["merchants"], "preferred_username": "dave",
          "iat": ..., "exp": ... (30 minutes), "auth_time": ..., "sid": "...", "jti": "..."}
```
In the browser flow Spring also sends a `nonce` and it comes back in the ID token (our manual script did not send one).
`groups` and `preferred_username` are not standard: our `FakeIdpConfig.tokenCustomizer` adds them. The *access* token has
the same issuer and subject, `scope: ["openid", "profile"]` and a shorter life (5 minutes).
What Spring checks for an ID token: the **signature** (with the public key of the IdP, found through the `kid` in the
JWK set `/oauth2/jwks`), the **issuer** (`iss` is the provider that we configured), the **audience** (`aud` contains our
`client_id`), the **expiry** (with a small clock skew allowance), and the **nonce** (equal to the one in the session; it
ties the token to *this* login). `SsoSecurityConfig.ssoSuccessHandler` logs `iss`, `aud`, `sub`, `groups` and whether a nonce was
present, never the token itself: a token is a credential.

**5. Resource server for tokens of the IdP.** With the profile `idp`, `MultiIssuerAuthentication` makes `/api/**` accept
tokens of two issuers. The `iss` claim picks the `JwtDecoder`: our own (HS256, shared secret, roles in the claim `roles`)
or the IdP (RS256, public keys fetched from `http://localhost:9000/oauth2/jwks`, the claim `groups` converted with
`SsoRoleMapper`). The map of trusted issuers is an allow-list. Proven by `MultiIssuerAuthenticationTest` (a token of the
kingdom, a token of the IdP, an unknown issuer, a forged RSA token, the kingdom's secret used to forge an "IdP" token, an
RSA token that claims to be ours) and by a real IdP token: bob (guards) `GET /api/persons` 200, `DELETE` 403; alice
(royal-court) `DELETE` 404 (allowed); a token with a changed payload 401; a token with `alg: none` and an unknown issuer 401.

*Which beans of `JwtConfig` would you no longer need with *only* the IdP?* The `SecretKey`, the `JwtEncoder` and the HS256
`JwtDecoder` (and so `JwtService`, `AuthController.login` and the refresh tokens): the kingdom no longer issues tokens, it
only validates them. The `JwtAuthenticationConverter` would read `groups` instead of `roles`.
*The advantage over a shared secret:* with HMAC every service that can *validate* a token can also *forge* one; with an
asymmetric signature the API only has public keys. Keys can rotate (a new `kid` in the JWK set, no restart or secret to
distribute). And the users, their passwords, MFA, blocking and off-boarding are managed in one place.

**6. Why does the kingdom issue its own token?** It keeps the application independent of the IdP: its own claims (`roles`
instead of `groups`), its own lifetime and one token type for the frontend, whether the user logged in with a password or
through SSO. The drawbacks: one more component to secure (the signing secret); identity data is duplicated; the lifetime
is independent of the IdP (a user who is disabled at the IdP keeps working until our token expires; roles are frozen as
in B2); and the token travels in a URL fragment to the frontend (a one-time code that the frontend exchanges is safer).
**Single logout:** logging out of the kingdom only forgets *our* token. The session at the IdP (cookie `FAKEIDP_SESSION`)
remains, so clicking the SSO button again logs in without asking a password. Real single logout needs OpenID Connect
*RP-initiated logout* (the `end_session_endpoint` in the discovery document: `http://localhost:9000/connect/logout`) and
optionally *back-channel logout* (the IdP notifies each application), plus revoking the refresh tokens.

**7. "OAuth is not authentication".** *Authorization* (OAuth 2.0) is about what an application may do for a user: the
**access token** (here a JWT, but formally opaque to the client) is the key to a resource. It does not tell the client
*who* the user is. *Authentication* (OpenID Connect, built on OAuth) answers "who logged in": the **ID token**, a JWT
signed by the IdP, for *our* client (`aud`), bound to this login (`nonce`), with the identity in `sub`. In this flow the ID
token logs the user in, and our own token then authorizes the calls to the API.

---

## Part C: JPA inheritance and DTOs

### C1 A new subclass in SINGLE_TABLE

Code: `domain/Peasant` (`@DiscriminatorValue("PEASANT")`, field `crop`), `PersonDto.Peasant` (also in `permits` and
`@JsonSubTypes`), the two cases in `Mappers`. Tests: `InheritanceMappingTest.aNewSubclassIsJustAnotherValueInTheDiscriminatorColumn`,
`PersonRestControllerTest.testCreateAPeasant`.

- In the database nothing is created: a new value (`PEASANT`) in `person_type`, one new nullable column `crop`. For a
  peasant the columns of knights and wizards are `NULL`, and for a knight `crop` is `NULL`.
- `PersonDto` is `sealed`: `Mappers.toEntity` has no `default`, so the compiler fails as soon as a permitted subtype is
  not handled. **Careful:** `Mappers.toDto(Person)` *does* have a `default` (for plain persons), so a forgotten entity
  type there falls through to `PersonDto.Plain` and silently loses its fields. The test on `crop` is what catches it.
- A person's type cannot change (`ArmyServiceImpl.updatePerson` compares the classes and answers 400).

### C2 From JOINED to SINGLE_TABLE

Tried for real (and changed back). With `@Inheritance(strategy = SINGLE_TABLE)` and no `@Table` on `Weapon` and `Armor`:

**1. The generated table**

```sql
create table equipment (
    damage integer not null, defense integer not null,        -- NOT NULL!
    weight float(53) not null, created_at timestamp(6) not null, id bigint not null,
    dtype varchar(31) not null check ((dtype in ('Armor','Weapon'))),
    damage_type varchar(255) not null, material varchar(255) not null, name varchar(255) not null,
    primary key (id))
```
There is a discriminator column `dtype` (the default name) with the entity names as values. **The subclass columns are
`NOT NULL`**, which is a trap: a `Weapon` has no `defense`, so inserting one fails with *"NULL not allowed for column
DEFENSE"* (two tests fail). Two things cause it: the explicit `@Column(nullable = false)` is *not* ignored in a
SINGLE_TABLE subclass, and a **primitive** `int` is `NOT NULL` anyway. The fix: wrapper types (`Integer damage`) and no
`nullable = false`; then the columns become nullable (`damage integer`, `damage_type varchar(255)`).

**2. Tests that fail afterwards** (`InheritanceMappingTest`, after the fix above), two:
- `joinedSplitsTheColumnsOverParentAndChildTables`: *Table "WEAPON" not found*: there are no child tables anymore.
- `joinedSubclassColumnsCanBeNotNull`: *expected an exception, but nothing was thrown*: the database no longer enforces that
  `damage_type` is not null. The rule has to move to the application (Bean Validation on the DTO, as in C3).
`ArsenalRestControllerTest` stays green: the API does not care how the data is stored.

**3. The SQL of `findAll()`**

```sql
-- JOINED                                          -- SINGLE_TABLE
select e1_0.id,                                    select e1_0.id, e1_0.dtype, e1_0.created_at, e1_0.name,
  case when e1_1.id is not null then 1                    e1_0.weight, e1_0.defense, e1_0.material,
       when e1_2.id is not null then 2 end,               e1_0.damage, e1_0.damage_type
  e1_0.created_at, e1_0.name, e1_0.weight,         from equipment e1_0
  e1_1.defense, e1_1.material,
  e1_2.damage, e1_2.damage_type
from equipment e1_0
left join armor  e1_1 on e1_0.id = e1_1.id
left join weapon e1_2 on e1_0.id = e1_2.id
```
JOINED needs a `left join` per subclass and works out the class with a `case`; a query on one subclass joins its child table.
SINGLE_TABLE is one table scan and the class is in `dtype`; a query on one subclass is `where dtype = 'Weapon'`.

**4. When which?**
- **SINGLE_TABLE:** the default and the fastest (no joins, one insert per entity, simple polymorphic queries). Choose it
  when the subclasses have few extra columns and you can live with them being nullable in the database (validate in the
  application). Drawbacks: a wide table with many `NULL`s, no `NOT NULL` or foreign keys on subclass columns at database level.
- **JOINED:** a normalised schema: every column in the table where it belongs, `NOT NULL` and constraints work per
  subclass. Choose it when subclasses have many columns of their own that must be mandatory, or the data is reported on
  directly in SQL. Drawbacks: joins on every query (slower with deep hierarchies) and two inserts per entity.
- The course book's advice: start with SINGLE_TABLE, choose JOINED when subclass columns must be NOT NULL, avoid
  TABLE_PER_CLASS (UNIONs, no IDENTITY keys).

### C3 Design it yourself

Code: `domain/Building` (`SINGLE_TABLE`, `building_type`) with `Castle`, `Farm`, `Tower`, `dto/BuildingDto` (sealed, with
`@NotNull`/`@Positive` on the subclass fields), `BuildingRepository`, `controller/BuildingRestController` (`GET`: everybody
who is logged in, `POST`: only the king), the seed in `DataInitializer`. Tests: `BuildingRestControllerTest`,
`InheritanceMappingTest.buildingsLiveInOneTableWithTheirOwnDiscriminator`.

**The choice, SINGLE_TABLE, and why:** each subclass has only two own columns, so the table stays narrow (about 8 columns, 5 of
them nullable); the most common query is "all buildings", which is one select without joins; and no subclass column has to
be `NOT NULL` *in the database*: the rules ("a castle has towers") are checked on the way in by the DTO. The fields are
wrapper types (`Integer`, `Boolean`) so they can be `NULL`. JOINED would be better if the buildings had a dozen mandatory columns
each. The test shows the result: one table `building`, no table per subclass, `towers` is `NULL` for a farm.

### C4 An association with polymorphism

Code: `Knight.equipment` (`@OneToMany` + `@JoinColumn(name = "knight_id")`: the column `knight_id` is in the table
`equipment`), `KnightRepository.findAllWithEquipment` (`@EntityGraph`) and `findByEquipmentId`, `ArmyService.equipmentOf`,
`giveEquipment`, `knightsWithEquipment`, `GET /api/persons/{name}/equipment`, `POST /api/persons/{name}/equipment/{id}`,
`GET /api/knights`. Tests: `KnightEquipmentQueryTest`, `PersonRestControllerTest`, `ArmyServiceImplTest`.

**2. The N+1 problem, proven.** `KnightEquipmentQueryTest` counts the statements with the Hibernate statistics:
`knightRepository.findAll()` followed by `knight.getEquipment().size()` for each knight costs **1 + N** statements (1 select for the
knights, then one select per knight for the lazily loaded list). With 2 seeded knights that is 3; with 1000 knights 1001.

**3. The fix:** `@EntityGraph(attributePaths = "equipment")` on a `@Query("select k from Knight k order by k.name")`: **1**
statement (a `left join` on `equipment`), however many knights there are. (Hibernate de-duplicates the knights, the test
asserts the same number of knights.) `join fetch` in the JPQL does the same; use `@EntityGraph` when you want to keep a
derived query or reuse the query with different graphs.

**4. `LazyInitializationException`, and the DTO.** A `@OneToMany` is lazy by default: the list is a proxy that loads when
you touch it, which needs an open session. Outside a transaction (and with `open-in-view: false`, as here) the entity is
detached as soon as the repository call returns, and touching the list throws `LazyInitializationException`
(`KnightEquipmentQueryTest.outsideATransactionTheLazyCollectionCannotBeLoaded`). Returning the entity from the controller
would make Jackson walk into the lazy list during serialization, in the web layer, outside the transaction: an exception,
or with `open-in-view: true` (Spring Boot's default!) a silently executed extra query per entity in the view layer, which
also holds a database connection during the whole request. A DTO solves it by design: the service decides *inside* the
transaction exactly what is loaded (`equipmentOf` copies the list while the transaction is open; `findAllWithEquipment`
fetches it), the controller only maps plain data, and Jackson never touches a lazy proxy. It also stops a bidirectional
relationship from causing infinite recursion in the JSON.

*Also:* a piece of equipment can only be carried by one knight; giving one that is taken gives 409 (`ConflictException`).
Deleting a knight keeps his equipment: Hibernate sets `knight_id` to `NULL` first (unidirectional `@OneToMany` with a
join column), and the equipment can be given to somebody else (`ArmyServiceImplTest.deletingAKnightKeepsHisEquipment`).

### C5 Validation and error handling

Code: `PersonDto.Knight` (`@NotBlank` on `knightRank`, `@Size(max = 30)` on `horseName`),
`GlobalExceptionHandler.handleMethodArgumentNotValid`. Test: `PersonRestControllerTest.testAKnightNeedsARankAndAShortHorseName`.

- A `POST` without `knightRank` gives **400** with `X-Error-Message: knightRank: A knight needs a rank`; a horse name of 31
  characters gives `horseName: The name of the horse can have at most 30 characters`. The handler turns the field errors
  into one text (`field: message; field: message`). The frontend shows this text in the red box under the form.
- The validation sits on the **DTO**, not on the entity: it describes what the API accepts. The entity stays a database
  mapping (and, as C2 shows, the database does not enforce it for SINGLE_TABLE).
- The alternative is a JSON answer with `ProblemDetail` (RFC 9457, `application/problem+json`: `type`, `title`, `status`,
  `detail`, plus your own properties such as a list of `errors`). It is the standard format for API errors and Spring
  supports it out of the box (`ResponseEntityExceptionHandler` already produces it); this application answers with the
  http.cat picture and the header on purpose, as the course does.

### C6 Business rules in the service

Code: `domain/Profession` (`BLACKSMITH(18)` added), `ArmyServiceImpl.trainPerson` and `releasePerson`,
`DELETE /api/persons/{name}/training`, `exception/ConflictException` (409). Tests: `ArmyServiceImplTest`,
`PersonRestControllerTest`, `SecurityTest.onlyKingsAndKnightsMayTrainPersons`.

1. `BLACKSMITH(18)`: `POST /api/persons/{name}/training` with `{"profession": "BLACKSMITH"}`.
2. *A wizard can never be a soldier* gives **400** (`"A wizard cannot become a SOLDIER"`). **Where does the rule belong?**
   In the **service**. The controller only translates HTTP (it would be duplicated by the next entry point: a scheduler, a
   message); a rule in the entity is possible for simple invariants, but this rule also needs the *current state* and
   other collaborators, and it belongs next to the transaction that applies it. The service can be unit tested without a web
   layer.
3. *Only a person without a profession can become a soldier* gives **409 Conflict**; `DELETE .../training` releases the person
   first. **400 or 409?** 400 means "this request is invalid" and will never work (a wizard as a soldier); 409 means "the
   request is fine, but the *current state* of the resource does not allow it" and it can succeed later (after the release).
   Both are defensible, but the difference tells the client what to do.
4. **Why does `trainPerson` not call `save()`, and why is it stored anyway?** `personRepository.findByNameIgnoreCase` is called
   inside the `@Transactional` method of the class, so the returned `Person` is a **managed** entity: it belongs to the
   persistence context, which keeps a snapshot. When the transaction commits, Hibernate does *dirty checking*, sees that
   `profession` changed and sends the `UPDATE` itself. `save()` on a managed entity would do nothing extra.
   **Without `@Transactional`** every repository call is its own short transaction, and the entity is *detached* as soon as
   `findBy...` returns. `setProfession` then changes only a Java object that nobody is watching: no `UPDATE`, nothing is
   stored, and (the nasty part) the method still returns the changed object, so the answer *looks* right. The tests find it
   because they look the person up again (`trainPersonGivesAnExistingPersonAProfession`).
