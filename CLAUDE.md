# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

GestoJob is a personal job-application tracker: a server-rendered Spring Boot + Thymeleaf web app (no SPA; the only REST surface is the token-protected `/api/**` used by the `gestion-annonce` Claude Code skill, see "Claude API") used by two job seekers (Laurent Touret and Renée, each with fully separate data) to manage companies (`entreprises`), contacts, and job applications (`annonces`), and to send cover-letter emails. The domain language is **French** — keep new code, field names, and UI text in French to match.

Stack: Spring Boot 4.0.2, Java 17, Maven, PostgreSQL, Liquibase, Thymeleaf, Lombok, jsoup, Jakarta Mail (Gmail SMTP).

## Commands

```bash
# Build / test (use the wrapper)
./mvnw clean package           # full build + tests
./mvnw test                    # all tests
./mvnw test -Dtest=GestoJobApplicationTests#contextLoads   # single test

# Run locally (needs a running Postgres — see dev-start.sh)
./mvnw spring-boot:run

# Database only (dev): Postgres on :5432
./dev-start.sh                 # docker compose --profile dev up -d
./dev-stop.sh

# Full stack (prod-like): DB + app container, builds image
./prod-start.sh                # --profile fullstack up -d --build
./maj.sh                       # down + git pull + rebuild/up (deploy update)
```

The suite has ~327 tests across `model/`, `dto/`, `service/`, `controller/`, `tools/`, `securite/`, and `configuration/` — mostly fast JUnit 5 + Mockito unit tests (controllers tested directly, no MockMvc; services with mocked repositories). The lone exception is `GestoJobApplicationTests#contextLoads`, which boots the full Spring context and therefore requires a reachable database and the env vars below — so `./mvnw verify` needs Postgres running (`./dev-start.sh`). A new public method must come with matching tests (the `jacoco:check` rule flags coverage under 80 % line coverage — indicative, non-blocking: `haltOnFailure=false`).

## Configuration

Config is **not** in `application.yml` defaults — `application.yml` imports `optional:file:.env` and reads everything from environment variables. `.env` (gitignored in practice; `dist.env` is the template) must define: `POSTGRES_DB/USER/PASSWORD`, `MAIL_LAURENT_USERNAME/PASSWORD` (`MAIL_RENEE_*` facultatives), `DOCKER_UI_PORT` (also the app's `server.port`), and `MAX_*_PAR_PAGE` pagination sizes. `GESTOJOB_API_TOKEN` (→ `gestojob.api.token`) is the Claude API token (≥ 32 random bytes); empty = API disabled.

`application-docker.yml` (activated by `SPRING_PROFILES_ACTIVE=docker` in the container) is layered over `application.yml` and differs on: the datasource host (`gestojob-db` instead of `localhost`), `server.forward-headers-strategy: framework` (instead of `native`) and secure/http-only/same-site session cookies (the app sits behind an HTTPS reverse proxy). The `gestojob.api.token` and journal settings come from `application.yml`.

## Architecture

Standard layered MVC under `com.mr486.gestojob`:

- `controller/` — `@Controller` classes returning Thymeleaf view names (`accueil`, `annonces`, `entreprises`, `file`, `parametres`). POST endpoints mutate then `redirect:` (PRG pattern). `ContenuController` is a `@RestController` serving plain-text cover-letter content at `/contenu/{id}`; the other `@RestController` is the API one under `controller/api/` (see "Claude API"). `TitulaireConseil` (`@ControllerAdvice`) exposes `titulaireAffichage` to every view; `ErreurConseil` (`@ControllerAdvice`) turns `NoSuchElementException` (unknown id, or one owned by the other titulaire — same response) into a 404 page (`erreur-404`), and `ContenuController` has its own plain-text 404 handler.
- `service/` — business logic. `AnnonceService` is the core orchestrator (read, delete, search/pagination), split with `AnnonceCreationService` (creation, with or without a personalised letter), `AnnonceStatutService` (status transitions), `AnnonceTexteService` and `AnnonceMailService` (email sending). `ContenuService` holds the cover-letter templates as Java text blocks (HTML + plain-text variants) and does `{{POLITESSE}}`/`{{POSTE}}` substitution.
- `persistance/` — Spring Data JPA repositories (note the French spelling of the package).
- `model/` — JPA entities (`Entreprise`, `Contact`, `Annonce`) and domain enums (`StatutAnnonce`, `TypeAnnonce`, `TypeContenu`).
- `dto/` — form-backing objects (`*Form`) and view rows (`*Liste`).
- `tools/` — `MailTools` (MimeMessage HTML mail) and `HtmlToPlainText` (jsoup-based HTML→text).
- `configuration/ApplicationConfiguration` — centralized **business constants** (civilities, generic salutation; the candidate name, letter labels and default job titles are per-titulaire data in the `profil` table). Put new business constants here, not as scattered literals (deployment/secret config stays in `.env`).

### Multi-user (titulaires and lecteurs)

Four fixed in-memory accounts (`SecurityConfig`, credentials from `GESTOJOB_{LAURENT,RENEE,FT01,FT02}_{USERNAME,PASSWORD}` in `.env`): `laurent` and `renee` are **titulaires** (`ROLE_TITULAIRE`, read/write their own data); `ft01` / `ft02` are France Travail **lecteurs** (`ROLE_LECTEUR`, read-only mirror of laurent / renee). Each account also carries `DONNEES_<TITULAIRE>`, which fixes whose data it sees (the mapping ft01→LAURENT, ft02→RENEE is structural, not configurable).

- `model.Titulaire` (`LAURENT`, `RENEE`) is the stable code stored in the `titulaire` column of `entreprises`, `contacts`, `annonces`, `modele_email` and as primary key of `profil` (not the login).
- `securite.UtilisateurCourant.titulaire()` resolves the titulaire from the authentication. **Rule: every repository query on business data is filtered by titulaire** (derived `…AndTitulaire` methods or explicit `@Query` with `:titulaire`); never use inherited `findById/findAll/count/deleteById` on `Entreprise`/`Contact`/`Annonce`/`ModeleEmail` (only `save`, or the `ProfilRepository` keyed by titulaire). An id belonging to the other titulaire must behave exactly like an unknown id. Controllers never take the titulaire from a request parameter.
- Read-only is enforced server-side in `SecurityConfig` (home `/` requires login; non-GET requests, creation forms and the whole Paramètres page require `TITULAIRE`; 403 otherwise), and the UI hides action buttons with `sec:authorize`. `TitulaireConseil` (`@ControllerAdvice`) exposes only `titulaireAffichage` (the current titulaire's display name) to views.
- `ProfilService` / `profil` table: candidate name plus label and default job title for each of the three letter types, editable in Paramètres. Default email templates are per titulaire under `src/main/resources/modeles/defaut/{LAURENT,RENEE}/`. A letter template (`CONTENU_*`) must contain `{{POLITESSE}}` and `{{POSTE}}` (`CleModele.getVariablesObligatoires`): Paramètres offers insert buttons and blocks the save in the browser, and `ModeleEmailService.mettreAJour` refuses it server-side.
- `MailConfiguration` builds one SMTP sender per titulaire. Host/port default to `spring.mail.*` (Gmail) and can be overridden per titulaire with `gestojob.mail.<titulaire>.host|port` (renée uses Yahoo: `smtp.mail.yahoo.com:587`). `MAIL_LAURENT_*` required, `MAIL_RENEE_*` optional (without them renée's annonces stay queued and sending reports "adresse d'envoi non configurée").

### Claude API (`/api/**`)

Token-authenticated REST API used by the `gestion-annonce` Claude Code skill, serving **LAURENT only**.

- `configuration/ApiSecurityConfig` is a second, higher-priority `SecurityFilterChain` matching `/api/**` only: stateless, CSRF off for this chain only, no form login. `SecurityConfig`'s form chain is untouched. `securite/JetonApiFiltre` reads header `X-GestoJob-Jeton`, compares in constant time to `gestojob.api.token` and authenticates as `ROLE_TITULAIRE` + `DONNEES_LAURENT`, so `UtilisateurCourant` and every service work unchanged. Missing/wrong token (or empty configured token) → JSON 401 plus exactly one `ECHEC_CONNEXION identifiant="api" adresse=<ip>` line on logger `securite.connexion` (picked up by fail2ban). Never log the token or header.
- `controller/api/CandidatureApiController` (`@RestController`): `GET /api/modele-lettre`, `GET /api/entreprises?nom=`, `POST /api/candidatures` (201). Logic lives in `service/CandidatureApiService` (reuses `EntrepriseService`, `ContactService`, `AnnonceCreationService.creerAvecLettre`); DTOs in `dto/api/`. `controller/api/ApiErreurConseil` returns JSON `{ "erreur": ... }` (400 business/validation/unreadable body, 404 `NoSuchElementException`, 405, 415, and a catch-all 500 `{"erreur":"Erreur interne."}` logged without the request body) so the HTML `ErreurConseil` does not apply.
- The API never sends mail and never updates or deletes an annonce. Created annonces use `typeContenu = TypeContenu.IA.getCode()`; with a contact they are queued (status 1), without one they are "site" (status 2).
- `Annonce.lettrePersonnalisee` (column `lettre_personnalisee`, migration `009`): when non-empty, `ContenuService` uses it **instead of the template** (HTML and plain text, hence email and `/contenu/{id}`). It is sanitised with `HtmlSanitizer.nettoie` and must contain `{{POLITESSE}}`; `{{POSTE}}` is optional.

### Database / migrations

Schema is owned by **Liquibase**, not Hibernate (`spring.jpa.hibernate.ddl-auto: validate`). Any schema change requires a new SQL changelog in `src/main/resources/db/changelog/` registered in `master.xml` — do not rely on entity changes to alter the DB. Migration `008-multi-utilisateurs.sql` adds the `titulaire` columns (existing data goes to LAURENT) and the `profil` table. (One existing file is named `003-creation_annonces.sql.sql` — the double extension is intentional/existing.)

### Magic-number conventions (important)

The domain encodes state as integer codes, now centralized in enums under `model/` (`StatutAnnonce`, `TypeAnnonce`, `TypeContenu`), each carrying the code + libellé. Use `EnumX.Y.getCode()` in services/code instead of bare literals; JPQL `@Query` strings still hold the literal (commented). Codes:

- `statusAnnonce` (`StatutAnnonce`): 1 = boîte d'envoi (queued for email), 2 = en cours (sent), 3 = dépassé, 4 = négatif, 5 = positif, 6 = archivé.
- `typeAnnonce` (`TypeAnnonce`): 0 = spontanée (S), 1 = candidature à une référence (A) — drives `getLibelle()` subject.
- `typeContenu` (`TypeContenu`): 1 = microservices, 2 = IA agentique, else = général template.

An annonce with no `contactId` is treated as a "site" application: created already-sent (`status=2`, `dateEnvoi=now`); with a contact it is queued (`status=1`) for later email dispatch via the file d'attente (`/file`).
