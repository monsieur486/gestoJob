# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

GestoJob is a personal job-application tracker: a server-rendered Spring Boot + Thymeleaf web app (no SPA, no REST API for clients) used by two job seekers (Laurent Touret and Renée, each with fully separate data) to manage companies (`entreprises`), contacts, and job applications (`annonces`), and to send cover-letter emails. The domain language is **French** — keep new code, field names, and UI text in French to match.

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

The suite has ~273 tests across `model/`, `dto/`, `service/`, `controller/`, `tools/`, and `configuration/` — mostly fast JUnit 5 + Mockito unit tests (controllers tested directly, no MockMvc; services with mocked repositories). The lone exception is `GestoJobApplicationTests#contextLoads`, which boots the full Spring context and therefore requires a reachable database and the env vars below — so `./mvnw verify` needs Postgres running (`./dev-start.sh`). A new public method must come with matching tests (the `jacoco:check` rule flags coverage under 80 % line coverage — indicative, non-blocking: `haltOnFailure=false`).

## Configuration

Config is **not** in `application.yml` defaults — `application.yml` imports `optional:file:.env` and reads everything from environment variables. `.env` (gitignored in practice; `dist.env` is the template) must define: `POSTGRES_DB/USER/PASSWORD`, `MAIL_LAURENT_USERNAME/PASSWORD` (`MAIL_RENEE_*` facultatives), `DOCKER_UI_PORT` (also the app's `server.port`), and `MAX_*_PAR_PAGE` pagination sizes.

`application-docker.yml` (activated by `SPRING_PROFILES_ACTIVE=docker` in the container) is identical except the datasource host is `gestojob-db` instead of `localhost`.

## Architecture

Standard layered MVC under `com.mr486.gestojob`:

- `controller/` — `@Controller` classes returning Thymeleaf view names (`accueil`, `annonces`, `entreprises`, `file`, `parametres`). POST endpoints mutate then `redirect:` (PRG pattern). `ContenuController` is the lone `@RestController`, serving plain-text cover-letter content at `/contenu/{id}`. `TitulaireConseil` (`@ControllerAdvice`) exposes `titulaireAffichage` to every view; `ErreurConseil` (`@ControllerAdvice`) turns `NoSuchElementException` (unknown id, or one owned by the other titulaire — same response) into a 404 page (`erreur-404`), and `ContenuController` has its own plain-text 404 handler.
- `service/` — business logic. `AnnonceService` is the core orchestrator (CRUD, status transitions, email sending, search/pagination). `ContenuService` holds the cover-letter templates as Java text blocks (HTML + plain-text variants) and does `{{POLITESSE}}`/`{{POSTE}}` substitution.
- `persistance/` — Spring Data JPA repositories (note the French spelling of the package).
- `model/` — JPA entities (`Entreprise`, `Contact`, `Annonce`) and domain enums (`StatutAnnonce`, `TypeAnnonce`, `TypeContenu`).
- `dto/` — form-backing objects (`*Form`) and view rows (`*Liste`).
- `tools/` — `MailTools` (MimeMessage HTML mail) and `HtmlToPlainText` (jsoup-based HTML→text).
- `configuration/ApplicationConfiguration` — centralized **business constants** (civilities, generic salutation; the candidate name, letter labels and default job titles are per-titulaire data in the `profil` table). Put new business constants here, not as scattered literals (deployment/secret config stays in `.env`).

### Multi-user (titulaires and lecteurs)

Four fixed in-memory accounts (`SecurityConfig`, credentials from `GESTOJOB_{LAURENT,RENEE,FT01,FT02}_{USERNAME,PASSWORD}` in `.env`): `laurent` and `renee` are **titulaires** (`ROLE_TITULAIRE`, read/write their own data); `ft01` / `ft02` are France Travail **lecteurs** (`ROLE_LECTEUR`, read-only mirror of laurent / renee). Each account also carries `DONNEES_<TITULAIRE>`, which fixes whose data it sees (the mapping ft01→LAURENT, ft02→RENEE is structural, not configurable).

- `model.Titulaire` (`LAURENT`, `RENEE`) is the stable code stored in the `titulaire` column of `entreprises`, `contacts`, `annonces`, `modele_email` and as primary key of `profil` (not the login).
- `securite.UtilisateurCourant.titulaire()` resolves the titulaire from the authentication. **Rule: every repository query on business data is filtered by titulaire** (derived `…AndTitulaire` methods or explicit `@Query` with `:titulaire`); never use inherited `findById/findAll/count/deleteById` on `Entreprise`/`Contact`/`Annonce`/`ModeleEmail` (only `save`, or the `ProfilRepository` keyed by titulaire). An id belonging to the other titulaire must behave exactly like an unknown id. Controllers never take the titulaire from a request parameter.
- Read-only is enforced server-side in `SecurityConfig` (home `/` requires login; non-GET requests and creation forms require `TITULAIRE`; 403 otherwise), and the UI hides action buttons with `sec:authorize`. `TitulaireConseil` (`@ControllerAdvice`) exposes only `titulaireAffichage` (the current titulaire's display name) to views.
- `ProfilService` / `profil` table: candidate name plus label and default job title for each of the three letter types, editable in Paramètres. Default email templates are per titulaire under `src/main/resources/modeles/defaut/{LAURENT,RENEE}/`.
- `MailConfiguration` builds one SMTP sender per titulaire. Host/port default to `spring.mail.*` (Gmail) and can be overridden per titulaire with `gestojob.mail.<titulaire>.host|port` (renée uses Yahoo: `smtp.mail.yahoo.com:587`). `MAIL_LAURENT_*` required, `MAIL_RENEE_*` optional (without them renée's annonces stay queued and sending reports "adresse d'envoi non configurée").

### Database / migrations

Schema is owned by **Liquibase**, not Hibernate (`spring.jpa.hibernate.ddl-auto: validate`). Any schema change requires a new SQL changelog in `src/main/resources/db/changelog/` registered in `master.xml` — do not rely on entity changes to alter the DB. Migration `008-multi-utilisateurs.sql` adds the `titulaire` columns (existing data goes to LAURENT) and the `profil` table. (One existing file is named `003-creation_annonces.sql.sql` — the double extension is intentional/existing.)

### Magic-number conventions (important)

The domain encodes state as integer codes, now centralized in enums under `model/` (`StatutAnnonce`, `TypeAnnonce`, `TypeContenu`), each carrying the code + libellé. Use `EnumX.Y.getCode()` in services/code instead of bare literals; JPQL `@Query` strings still hold the literal (commented). Codes:

- `statusAnnonce` (`StatutAnnonce`): 1 = boîte d'envoi (queued for email), 2 = en cours (sent), 3 = dépassé, 4 = négatif, 5 = positif, 6 = archivé.
- `typeAnnonce` (`TypeAnnonce`): 0 = spontanée (S), 1 = candidature à une référence (A) — drives `getLibelle()` subject.
- `typeContenu` (`TypeContenu`): 1 = microservices, 2 = IA agentique, else = général template.

An annonce with no `contactId` is treated as a "site" application: created already-sent (`status=2`, `dateEnvoi=now`); with a contact it is queued (`status=1`) for later email dispatch via the file d'attente (`/file`).
