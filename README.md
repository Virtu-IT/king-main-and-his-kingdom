# King Main and his Kingdom

Spring Boot 4 demo for the course: REST, JPA (including inheritance), Spring Security and an Angular frontend that starts together with the application.

## Requirements

Only a **JDK 21** and **IntelliJ** (with its built-in Maven). No Docker, no database and no Node.js: the database is H2 in memory and the frontend is already built. Internet is not required (without internet only the http.cat pictures on errors are missing).

## Project structure

A Maven multi-module project. Open the **root folder** in IntelliJ (it imports both modules).

| Module | What |
|---|---|
| `kingdom/` | the application of the course: REST, JPA, Spring Security and the Angular frontend (`kingdom/frontend`) |
| `fake-idp/` | a separate, tiny application that plays the identity provider for the single sign-on exercise; you do not need it for anything else |

All paths below (`config/DataInitializer`, `src/main/resources/static`, ...) are relative to `kingdom/`, unless stated otherwise.

## Running

```bash
mvn spring-boot:run -pl kingdom     # or start KingMainAndHisKingdomApplication in IntelliJ
```

Open <http://localhost:8080>. Node.js is not needed: the built Angular app is in `kingdom/src/main/resources/static`.

| URL | What |
|---|---|
| `/` | the Angular frontend |
| `/swagger-ui/index.html` | API documentation (Authorize: bearer token or basic) |
| `/h2-console` | H2 console (profile `local`, JDBC URL `jdbc:h2:mem:testdb`, user/password `test`) |

The profile `local` is on by default (SQL in the console, H2 console). For `prod`: `--spring.profiles.active=prod` and set `KINGDOM_SECURITY_JWT_SECRET`.
The demo users (logins, roles) are in `config/DataInitializer`; the frontend has buttons for them on the login page.

## JPA inheritance (`domain` package)

| Strategy | Classes | What you see |
|---|---|---|
| `@MappedSuperclass` | `BaseEntity` | `id` and `createdAt` are in every table, no table of its own |
| `SINGLE_TABLE` | `Person` / `Knight` / `Wizard` | one table `person` with a column `person_type`, subclass columns nullable |
| `JOINED` | `Equipment` / `Weapon` / `Armor` | `equipment` + `weapon` / `armor` with a shared primary key, subclass columns may be NOT NULL |
| `TABLE_PER_CLASS` | `WarMachine` / `Tank` / `Catapult` | no table for the parent, polymorphic query = UNION |

`InheritanceMappingTest` shows the tables and the polymorphic queries for each strategy. Also: derived queries, `@Query` with `type(e)`, and a custom repository fragment (`WarMachineRepositoryCustom`).

Recruiting and training are two steps: `POST /api/persons` recruits a new person (without a profession, king only); `POST /api/persons/{name}/training` selects an existing person from the database and trains them to a soldier, farmer or nurse (`Profession`, each with a minimum age; king and knights only).

The REST API never returns entities. It uses DTOs (`dto` package, sealed interfaces with `"type"` as the discriminator) and maps with `Mappers`.

## Spring Security (`security` package)

**Day 1: the basics**
- `SecurityConfig`: `SecurityFilterChain`, `authorizeHttpRequests` (public / logged in / role), two filter chains (`@Order`), `BCryptPasswordEncoder`
- 401 (not logged in) versus 403 (wrong role): `restAuthenticationEntryPoint` and `restAccessDeniedHandler`
- HTTP Basic (curl, Swagger), `WWW-Authenticate` and why the frontend sends `X-Requested-With`
- Method security: `@PreAuthorize` on `PersonRestController` and `ArsenalRestController`
- CORS (`corsConfigurationSource`) and why CSRF is off here (stateless, no cookie)
- Frontend: the **Security lab** page shows 200 / 401 / 403 / 404 per user

**Day 2: users in the database and JWT**
- `AppUser` + `JpaUserDetailsService`: users and BCrypt hashes in the database, `AccountService.register`
- `AuthController`: login returns a JWT (`JwtService`, `JwtConfig`: Nimbus encoder/decoder with HMAC), `/me`, `/register`
- Resource server: the token is validated, the claim `roles` becomes `ROLE_*` (`JwtAuthenticationConverter`)
- Typed configuration: `KingdomSecurityProperties`
- Testing: `SecurityTest` and `JwtLoginTest` (`httpBasic()`, `@WithMockUser`, expired / forged token)

**Single sign-on with a fake IdP (OAuth 2.0 / OpenID Connect)**

A real identity provider (Entra ID, Keycloak, ...) is not available on a locked-down laptop, so the project contains a fake IdP: the separate module `fake-idp` (Spring Authorization Server, everything in `fake-idp/src/main/java/com/bnp/fakeidp/FakeIdpConfig.java`).

1. Start the fake IdP (port 9000): `FakeIdpApplication` in IntelliJ, or `mvn spring-boot:run -pl fake-idp`.
2. Start the kingdom with the profile `idp`: in IntelliJ set *Active profiles* to `local,idp`, or `mvn spring-boot:run -pl kingdom -Dspring-boot.run.profiles=local,idp`.
3. On <http://localhost:8080/login> choose **Log in with the identity provider**. The users and groups of the IdP are in `FakeIdpConfig`.

The flow: `/oauth2/authorization/kingdom-idp`, login at the IdP, the code is exchanged for tokens (the ID token is validated with the keys of the IdP), `SsoRoleMapper` turns groups into roles, the kingdom issues its own JWT and the frontend picks it up (`/sso`). The code is in `SsoSecurityConfig` and `application-idp.yaml`. Without the profile `idp` the rest of the application does nothing with the IdP.

## Exercises

See [EXERCISES.md](EXERCISES.md): about 20 exercises on Spring Security (day 1 and 2), JPA inheritance and DTOs, with a suggested schedule.

## Changing the frontend (instructor only)

The Angular source (Angular 22, standalone components) is in `kingdom/frontend/`.

```bash
cd kingdom/frontend
npm install                  # once
npm start                    # dev server on http://localhost:4200, proxy to :8080
npm run publish-to-spring    # production build -> kingdom/src/main/resources/static (commit afterwards)
```

## Tests

```bash
mvn test                     # both modules
mvn test -pl kingdom         # only the kingdom
```

Tests use the profile `test` (their own in-memory database per context).
