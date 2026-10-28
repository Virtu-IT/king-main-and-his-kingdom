# Exercises

All exercises build on this project. Read the [README](README.md) first (structure, inheritance strategies, security classes).

The project has two modules: `kingdom` (all exercises happen here) and `fake-idp` (only used by exercise B8). Open the root folder in IntelliJ. All paths in the exercises (`config/DataInitializer`, `application.yaml`, ...) are relative to the `kingdom` module.

- ⭐ = core, 🔥 = challenging, 🎁 = bonus
- The times are estimates. A participant who does all ⭐ exercises and a few 🔥 ones is busy for well over two days.
- Always work **test-first or test-along**: every exercise has a "Done when" line that you can prove with a test (or `curl`).
- Run the tests with `mvn test` or in IntelliJ (right-click `src/test`, *Run Tests*). Start the app with `mvn spring-boot:run -pl kingdom` or through `KingMainAndHisKingdomApplication` in IntelliJ, and read the SQL in the console (profile `local`).

## What do you need?

Only a **JDK 21** and **IntelliJ** (which brings its own Maven). You do **not** need Docker, a database, Node.js or anything else installed:

- the database is H2 in memory (empty after every restart, filled by `DataInitializer`);
- the Angular frontend is a **finished product**: you do not need to know it or change it. All exercises are about the Java side; you only use the frontend to look at the result (test new endpoints with Swagger UI);
- all exercises work with the dependencies that are already in the `pom.xml`; nothing extra has to be downloaded.

Internet is not required: without internet only the http.cat pictures on errors are missing (the status code and text remain). You can work without `curl`: use Swagger UI (<http://localhost:8080/swagger-ui/index.html>, *Authorize* button) or the **Security lab** page in the frontend. Windows 10/11 usually has `curl.exe` built in.

The demo users are in `config/DataInitializer`. A curl example: `curl -u username:password http://localhost:8080/api/persons`.

---

## Part A: Spring Security, day 1

### A1 ⭐ Explore the security (30 min)
1. Call `/api/persons` without logging in, with Basic as `peasant` and with Basic as `king`. Which status codes do you get?
2. Try `DELETE /api/persons/Merlin` as `peasant`, as `lancelot` and as `king`. Why is that 403, 403 and 204?
3. Open Swagger UI, log in with `POST /api/auth/login`, press *Authorize* and paste the token.
4. Open the **Security lab** in the frontend. First predict the result per row for each user, then check.
5. Set `kingdom.security.jwt-ttl` to `30s` in `application.yaml`. What happens in the frontend after 30 seconds, and why?

**Done when:** you can explain the difference between 401 and 403 in your own words and point out in which class each answer is produced.

### A2 ⭐ A new endpoint with a URL rule (30 min)
Create `GET /api/stats` that returns a JSON object with the number of persons, pieces of equipment and war machines.
1. `/api/**` already protects it: everyone who is logged in may see it. Prove that with a test.
2. Restrict it in `SecurityConfig` with a **URL rule** to `KING` and `KNIGHT` (`requestMatchers(...).hasAnyRole(...)`).
3. Move the rule to a **`@PreAuthorize`** on the controller. When do you choose which of the two?

**Done when:** `peasant` gets 403, `lancelot` 200, anonymous 401, and you have a test for each.
*Careful:* the order of `requestMatchers` matters: the first match wins.

### A3 ⭐ A new role (45 min)
Introduce the role `STEWARD`. A steward may view and add `/api/equipment`, but may not see `/api/persons`.
1. Add a user `steward` in `DataInitializer` (via `AccountService.create`).
2. Change the rules. `GET /api/persons` now depends on the role, for example `hasAnyRole('KING','KNIGHT','SUBJECT')`.
3. The frontend also shows the *Persons* link to a steward. Why is that not a security problem, and why must the server enforce the rule and not the user interface?

**Done when:** a steward gets 403 on persons and 201 on new equipment, tested with `httpBasic("steward", ...)`.

### A4 ⭐ Ownership check with SpEL (45 min)
Create `GET /api/users/{username}` that shows the roles of a user.
- A user may only look up **themselves**, the king may look up anyone: `@PreAuthorize("#username == authentication.name or hasRole('KING')")`.
- Then create `PUT /api/users/{username}/password` (body: old and new password) that changes the password of the logged-in user. Check the old password with the `PasswordEncoder`. The new password gets the same validation as at registration.

**Done when:** `lancelot` cannot look up `peasant` (403), and after a password change only the new password works.
*Question:* do existing tokens stay valid after a password change? Is that a problem?

### A5 ⭐ Test the security (45 min)
Write a test matrix for `ArsenalRestController`: for `GET /api/equipment`, `POST /api/equipment`, `GET /api/war-machines` and `POST /api/war-machines` with (anonymous, SUBJECT, KNIGHT, KING).
Use `@ParameterizedTest` with `@MethodSource` or `@CsvSource`.

**Done when:** 16 combinations run in one test class and the expected codes (200/201/401/403) are correct.
*Tip:* you cannot parameterize `@WithMockUser(roles = "...")`; use `.with(user("x").roles(...))` from `SecurityMockMvcRequestPostProcessors`.

### A6 ⭐ Investigate CORS (30 min)
Without Node and without the Angular dev server: the JDK contains a simple web server (`jwebserver`, since Java 18) and `kingdom/exercises/cors-test.html` is a test page.
1. Start the application. Open a terminal in the project folder and start the test page on **another port**: `jwebserver -p 4200 -d kingdom/exercises` (it is in the `bin` folder of the JDK).
2. Open <http://localhost:4200/cors-test.html> and press the buttons. The origin `http://localhost:4200` is in `kingdom.security.cors-allowed-origins`. What do you see?
3. Open the same page through <http://127.0.0.1:4200/cors-test.html>. What is the difference and why (think of *origin* = protocol + host + port)?
4. Add `http://127.0.0.1:4200` in `application.yaml`, restart and try again.
5. Look in the network tab (F12) at the **preflight** (`OPTIONS`) of the call with a token. Which headers does the server send back?

**Done when:** you can explain why CORS is an agreement with the **browser** and does not protect the server (a call through Swagger UI or `curl` is not stopped).

### A7 🔥 Another password algorithm (45 min)
Replace `BCryptPasswordEncoder` by `PasswordEncoderFactories.createDelegatingPasswordEncoder()`.
1. What do you now see in the `password_hash` column in the H2 console (`{bcrypt}...`)?
2. Build your own `DelegatingPasswordEncoder` with `{pbkdf2}` as the default (`Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()`, already part of Spring Security: no extra dependency needed) where existing `{bcrypt}` hashes keep working.
3. Let a user with an old hash be upgraded automatically after a successful login (`UserDetailsPasswordService`).

**Done when:** a test proves that a `{bcrypt}` user can log in and that the hash then starts with `{pbkdf2}`.
*Discuss:* why is a slow, salted hash important, and why never MD5 or SHA-1 for passwords?

---

## Part B: Spring Security, day 2

### B1 ⭐ API key for an integration (60 min)
The course book (17.2) describes API keys. Build your own `OncePerRequestFilter`:
1. New endpoint `GET /api/integration/report` (number of persons).
2. A filter `ApiKeyAuthenticationFilter` reads the header `api-key` and compares it with `kingdom.security.api-key` (add that to `KingdomSecurityProperties`).
3. If the key is correct, put an `Authentication` with role `INTEGRATION` in the `SecurityContextHolder`. Otherwise: 401 through the existing `restAuthenticationEntryPoint`.
4. Add the filter in `SecurityConfig` before `BearerTokenAuthenticationFilter` (`addFilterBefore`) and limit it to `/api/integration/**`.

**Done when:** with the right key 200, without or with a wrong key 401, and a normal user without the role `INTEGRATION` gets 403.
*Think about it:* why should you compare keys with `MessageDigest.isEqual`?

### B2 ⭐ The king manages roles (60 min)
1. Create `PUT /api/admin/users/{username}/roles` (body: list of roles), for `KING` only. Use an `AccountService` method with `@Transactional`.
2. Validate: only known roles (`KING`, `KNIGHT`, `SUBJECT`, maybe `STEWARD`); the king may not remove their own `KING` role.
3. Test: promote `peasant` to `KNIGHT`.
4. **Investigate:** does `peasant` get the new rights immediately with their existing token? Why not? How would you solve that (short lifetime, refresh token, load roles from the database on every request)?

**Done when:** after logging in again `/api/auth/me` shows the new role, and you have written down the answer to question 4.

### B3 🔥 Account lock after failed logins (60 min)
1. Listen with `@EventListener` to `AuthenticationFailureBadCredentialsEvent` and `AuthenticationSuccessEvent`.
2. Count failed attempts per username (in-memory `ConcurrentHashMap`; how would you do it in a cluster?).
3. After 3 failed attempts the user is blocked for 1 minute: `/api/auth/login` then returns 429 with `Retry-After`. Use your own exception and `GlobalExceptionHandler`, or `UserDetails.isAccountNonLocked()`.
4. A successful login resets the counter.

**Done when:** a test does four wrong logins, the fifth one with the **correct** password is also refused, and after the time has passed (inject a `Clock`) it works again.
*Careful:* give an unknown user the same answer as a known one, otherwise you leak which accounts exist.

### B4 🔥 Refresh tokens (90 min)
The access token now lives for an hour. Make that shorter and add a refresh flow.
1. Set `kingdom.security.jwt-ttl` to 5 minutes; add `refresh-ttl` (for example 8 hours).
2. `POST /api/auth/login` now also returns a `refreshToken` (a random string, stored as a **hash** in a new table `refresh_token` with user and expiry date).
3. `POST /api/auth/refresh` exchanges a valid refresh token for a new access token **and** a new refresh token (rotation); the old one becomes invalid.
4. `POST /api/auth/logout` deletes the refresh token.
5. Test the whole flow with Swagger UI or in a test (log in, refresh, try the old token again). The frontend does not support refresh and does not need to.

**Done when:** reuse of an old refresh token gives 401, and you can explain why you store a refresh token as a hash.

### B5 ⭐ Audit log (30 min)
Log security events with an `@EventListener`:
- `AuthenticationSuccessEvent`, `AuthenticationFailureBadCredentialsEvent`
- `AuthorizationDeniedEvent` (a refused `@PreAuthorize`; switch the events on with an `AuthorizationEventPublisher` bean)

**Done when:** after `peasant` tries to delete someone there is a log line with who, what and when, **without** password or token.

### B6 🔥 Experience sessions and CSRF (90 min)
The API is stateless and switches CSRF off. Build a small "classic" part to see why that has to be different. Use **no** extra dependencies (so no Thymeleaf): a controller that returns HTML is enough.
1. An extra `SecurityFilterChain` with `@Order(1)` for `/legacy/**`: `formLogin()` with sessions and **CSRF on**.
2. A `@RestController` that returns `text/html`: `GET /legacy/hello` with a form that does `POST /legacy/note`. You get the CSRF token with a method parameter `CsrfToken token`; put it in a hidden field named `token.getParameterName()`.
3. First let the form post without the CSRF token: what is the answer? Then add the token and look at the `JSESSIONID` cookie.
4. Explain why this is not needed for the JWT API, and what can go wrong if you store a JWT in a cookie.

**Done when:** you can draw the difference between "logged in through a cookie" and "logged in through a header", with the attacker in the picture.

### B7 🎁 HTTPS (45 min)
`keytool` is part of the JDK, so there is nothing extra to install.
1. Create a keystore: `keytool -genkeypair -alias kingdom -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore kingdom.p12 -validity 365`.
2. In a new profile `https` set the `server.ssl.*` properties (see the course book 17.7: key store versus trust store).
3. Open <https://localhost:8443>. What does the browser say about the (self-signed) certificate? With `curl` you use `-k`.
4. Extra: `requiresChannel().anyRequest().requiresSecure()` or the HSTS header (`headers.httpStrictTransportSecurity`).

**Done when:** `https://localhost:8443` works and you can explain why Basic and JWT are unsafe without HTTPS.

### B8 ⭐ Logging in through an identity provider: OAuth 2.0 and OpenID Connect (90 min)
No real provider is needed: the project contains a **fake IdP** (module `fake-idp`, see the README) that runs on your own laptop.

**Starting up**
1. Start `FakeIdpApplication` of the module `fake-idp` first (IntelliJ, or `mvn spring-boot:run -pl fake-idp`; port 9000). Open <http://localhost:9000/.well-known/openid-configuration>: this is the *discovery document*.
2. Then start `KingMainAndHisKingdomApplication` with the profile `idp` (IntelliJ: *Active profiles* `local,idp`; Maven: `mvn spring-boot:run -pl kingdom -Dspring-boot.run.profiles=local,idp`).
3. Open <http://localhost:8080/login> and choose **Log in with the identity provider**. The users of the IdP (with their groups) are in `FakeIdpConfig` (module `fake-idp`).

**Tasks**
1. Follow the *authorization code flow* in the network tab (F12, "Preserve log" on): which redirects do you see, which `code` and `state` are in them, and which call fetches the tokens (the browser or the server)?
2. Log in as a user from each group. What is your role in the kingdom? Find the code that turns groups into roles (`SsoRoleMapper`) and explain why that logic lives **in the application** and not in the IdP.
3. Add a fourth user to `FakeIdpConfig` (module `fake-idp`) in a new group (for example `merchants`) and make that group `STEWARD` in the kingdom (see A3). Restart the IdP.
4. Get the ID token from `OidcUser` (breakpoint or log in `SsoSecurityConfig.ssoSuccessHandler`) with `user.getIdToken().getTokenValue()` and decode it (base64 of the middle part; the Security lab only shows the kingdom's own token). Which claims does it contain (`iss`, `aud`, `sub`, `nonce`, `groups`)? What does Spring check (signature through the JWKS, issuer, audience, expiry, nonce)?
5. 🔥 Turn the application into a **resource server for tokens of the IdP**: let `/api/**` accept an access token from `http://localhost:9000` directly (`spring.security.oauth2.resourceserver.jwt.jwk-set-uri` or `issuer-uri`, a second `JwtDecoder`, convert the `groups` claim with `SsoRoleMapper`). Which beans from `JwtConfig` do you no longer need then, and what is the advantage over a shared secret?
6. 🔥 Discuss: why does the kingdom issue its **own** token after the login, and what are the drawbacks? What happens to the session at the IdP when you log out of the kingdom (single logout)?
7. "OAuth is not authentication" (course book, 17.6): explain where in this flow the *authentication* is (OpenID Connect, ID token) and where the *authorization* is (OAuth, access token).

**Done when:** you can explain the flow to a colleague and you have let a user with a new group log in with the right role.
*Careful:* the fake IdP uses a new key at every start. If you restart the IdP, tokens issued earlier can no longer be validated. A real IdP has stable keys with rotation.

---

## Part C: JPA inheritance and DTOs

### C1 ⭐ A new subclass in SINGLE_TABLE (45 min)
Add `Peasant extends Person` with the field `crop` (for example "wheat").
1. Entity with `@DiscriminatorValue("PEASANT")`.
2. Change `PersonDto` (`PersonDto.Peasant`), `Mappers` and `@JsonSubTypes`. The `switch` over a `sealed` interface helps: the compiler tells you where you forgot something.
3. Try it with Swagger UI: `POST /api/persons` with `"type": "PEASANT"` (the frontend does not know this type and shows the details empty; that is not a problem).
4. Extend `InheritanceMappingTest`: one row, `person_type = 'PEASANT'`, other subclass columns `NULL`.

**Done when:** a POST with `"type": "PEASANT"` stores and returns a peasant, and `ArmyServiceImpl.updatePerson` does not allow the type to change.

### C2 ⭐ From JOINED to SINGLE_TABLE (45 min)
Change `Equipment` to `SINGLE_TABLE` (remove the `@Table` on `Weapon` and `Armor`).
1. Which columns are nullable now? What do the `@Column(nullable = false)` on `damage`, `defense` and `material` do? Start the application and look at the generated `create table`.
2. Which tests in `InheritanceMappingTest` fail and why? Adjust them.
3. Compare in the console the SQL of `GET /api/equipment` before and after (joins versus one table).
4. Write a paragraph: when do you choose JOINED and when SINGLE_TABLE?

**Done when:** the tests are green and you can defend the choice. Then change it back.

### C3 ⭐ Design it yourself (60 min)
The kingdom gets buildings: `Building` with `Castle` (towers, moat), `Farm` (hectares, crop) and `Tower` (height, crew).
1. Choose a strategy and **justify** it (number of subclass columns, NOT NULL, how often you query polymorphically).
2. Implement entities, repository, `BuildingDto`, mapper and `GET`/`POST /api/buildings` (read for everyone logged in, add for `KING`).
3. Write a test that shows which tables are created.

**Done when:** the endpoints work and you can explain your choice with the tables as proof.

### C4 🔥 An association with polymorphism (90 min)
A knight carries equipment: `Knight` gets a `@OneToMany` to `Equipment` (and a `@ManyToOne` back or a `@JoinColumn`).
1. Add `GET /api/persons/{name}/equipment`, returning a list of DTOs.
2. Switch `show-sql` on in the log and fetch all knights with their equipment. Do you see the **N+1 problem**?
3. Solve it with `@EntityGraph` or `join fetch` in a `@Query` and compare the number of queries.
4. Why would a `LazyInitializationException` leak if you returned an entity directly, and how does a DTO (and `open-in-view: false`) help?

**Done when:** a test with `Statistics` or the SQL log proves that the number of queries is constant after the fix.

### C5 ⭐ Validation and error handling (45 min)
1. Add validation to `PersonDto.Knight`: `knightRank` required, `horseName` at most 30 characters.
2. Adjust `GlobalExceptionHandler` so that validation errors per field end up in `X-Error-Message` (this partly happens already; test it) or choose a JSON answer with `ProblemDetail`.
3. Look at the result in Swagger UI and in the frontend (the *Recruit a person* form shows the reason on an error).

**Done when:** a POST without `knightRank` gives a 400 with a useful reason, tested in `PersonRestControllerTest`.

### C6 ⭐ Business rules in the service (45 min)
Recruiting adds a person (`ArmyService.recruitPerson`); training selects an existing person and gives them a profession (`ArmyService.trainPerson`, enum `Profession` with a minimum age).
1. Add the profession `BLACKSMITH` (minimum age 18) and train someone through Swagger UI (`POST /api/persons/{name}/training`).
2. Add a rule in `trainPerson`: a `Wizard` can never be trained as a `SOLDIER` (answer 400 with a clear message). Where does this rule belong: in the controller, in the service or in the entity? Why?
3. Add a rule: only a person who has no profession yet can be trained as a `SOLDIER` (to become a soldier after being a farmer, the person must first be released). Add `DELETE /api/persons/{name}/training` for that. Which HTTP status do you choose when a person with a profession is trained as a soldier: 400 or 409?
4. Why does `trainPerson` not call `personRepository.save(...)`, and why does the change still end up in the database? (Think of `@Transactional` and a *managed* entity.) What would happen without `@Transactional` on the class?

**Done when:** every rule has a test in `ArmyServiceImplTest`, and the endpoints are covered in `PersonRestControllerTest` and, for the roles, in `SecurityTest`.

---

## Suggested schedule

| Moment | Exercises | About |
|---|---|---|
| Day 1 morning | A1, A2, A3 | 1.5 hours |
| Day 1 afternoon | A4, A5, A6, (A7) | 2-2.5 hours |
| Day 2 morning | B1, B2, B5 | 2.5 hours |
| Day 2 afternoon | B3 or B4, C1, C4 | 3-4 hours |
| End of day 2 | B8 (fake IdP, OpenID Connect) | 1.5 hours |
| Fast groups | B6, B7, C2, C3, C5, C6 | the rest |

## Tips for the instructor
- The participants have no frontend knowledge: no exercise asks for changes in `frontend/`. They test new endpoints with Swagger UI; the frontend is only there to see the result.
- Everything runs locally and in memory: a restart resets the data to the seed, so experimenting is safe.
- If Maven in the bank environment cannot reach the central repository directly, let the participants run `mvn test` once through the internal mirror before the course. The exercises expect no new dependencies.
- Let participants write down their prediction first (which status code?) and only then run it.
- Spring Security 7 adds the authority `FACTOR_PASSWORD` after a login with a password. That is why `AuthController` and `JwtService` filter on the `ROLE_` prefix. A nice discussion about authorities versus roles.
- Common mistakes: the order of `requestMatchers`, `hasRole("ROLE_KING")` instead of `hasRole("KING")`, a `@PreAuthorize` on a private method or on a self-invocation (the proxy is bypassed), and forgetting `@EnableMethodSecurity`.
- The passwords in `DataInitializer` and `FakeIdpConfig` are for the demo only; stress that real secrets never belong in source code or in a frontend.
