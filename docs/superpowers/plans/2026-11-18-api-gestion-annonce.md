# API GestoJob et skill `gestion-annonce` — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** une API `/api/**` protégée par jeton qui permet à un skill Claude Code
(`gestion-annonce`) d'enregistrer en prod une candidature de Laurent avec une
lettre de motivation personnalisée par annonce.

**Architecture :** une seconde `SecurityFilterChain` sans état, limitée à `/api/**`,
authentifie un jeton (`X-GestoJob-Jeton`) comme le titulaire LAURENT, donc tous les
services existants (filtrés par `UtilisateurCourant`) fonctionnent tels quels. Un
service `CandidatureApiService` orchestre la création entreprise, puis contact, puis annonce
en une transaction. Une colonne `annonces.lettre_personnalisee` remplace le modèle dans
`ContenuService` quand elle est renseignée. Le skill vit dans `~/.claude/skills/`.

**Tech Stack :** Spring Boot 4.0.2, Java 17, Spring Security, Spring Data JPA,
Liquibase, Jackson 3 (`tools.jackson.*`), JUnit 5 + Mockito + AssertJ, Logback.

**Spec :** `docs/superpowers/specs/2026-11-18-api-gestion-annonce-design.md`

## Global Constraints

- Français partout : noms de classes, champs, messages, Javadoc (format du skill `javadoc-fr`), logs SLF4J.
- Requête métier toujours filtrée par titulaire. Jamais de `findById/findAll/count/deleteById` hérités. Le titulaire vient de `UtilisateurCourant`, jamais de la requête.
- Contrôleurs testés directement, sans MockMvc. Exception : la sécurité, testée dans `SecurityConfigTest` (`@SpringBootTest`, il faut Postgres : `./dev-start.sh`).
- Schéma : migration Liquibase `009-lettre-personnalisee.sql` enregistrée dans `master.xml`. Pas de `ddl-auto`.
- En-tête du jeton : `X-GestoJob-Jeton`. Propriété : `gestojob.api.token: ${GESTOJOB_API_TOKEN:}`. Une valeur vide désactive l'API.
- Ligne fail2ban, au caractère près, sur le logger `securite.connexion` : `ECHEC_CONNEXION identifiant="api" adresse=<ip>`. Une seule ligne par refus.
- Ne jamais journaliser le jeton, l'en-tête, ni le contenu des lettres.
- L'API n'envoie jamais de mail et ne modifie ni ne supprime aucune annonce.
- Annonce créée par l'API : `typeContenu = TypeContenu.IA.getCode()` (2).
- Lettre : passée par `HtmlSanitizer.nettoie`, doit contenir `{{POLITESSE}}`. `{{POSTE}}` est facultatif.
- Commits : jamais automatiques. Proposer le message (Conventional Commits en français, skill `git-commit`) et attendre le feu vert de Laurent. Pas de ligne `Co-Authored-By`, contributeur unique `monsieur486`.
- Travailler sur une branche `feat/api-gestion-annonce` (gros chantier → branche + PR).

## Review Focus

1. **Jeton configuré vide + en-tête vide ou absent → 401.** Une comparaison naïve `"".equals("")` ouvrirait l'API (test en tâche 3).
2. **Session de formulaire valide sans jeton sur `/api/**` → 401.** La chaîne API ne doit pas hériter de la session (test en tâche 3).
3. **`contact.id` d'une autre entreprise du même titulaire → 404**, et pas une annonce rattachée au mauvais contact (test en tâche 4).
4. **Recherche avec `nom` vide ou blanc → 400**, et pas la liste complète des entreprises (test en tâche 4).
5. **JSON mal formé ou bloc `annonce`/`entreprise` absent → 400 JSON `{ "erreur": … }`**, et pas la page HTML 404 ni une 500 (test en tâche 5).

---

### Task 1 : lettre personnalisée stockée et utilisée par `ContenuService`

**Files :**
- Create : `src/main/resources/db/changelog/009-lettre-personnalisee.sql`
- Modify : `src/main/resources/db/changelog/master.xml` (ajouter l'include après 008)
- Modify : `src/main/java/com/mr486/gestojob/model/Annonce.java` (champ)
- Modify : `src/main/java/com/mr486/gestojob/service/ContenuService.java:37-80`
- Modify : `src/main/java/com/mr486/gestojob/service/AnnonceMailService.java:150-162` (`buildHtmlContent`)
- Modify : `src/main/java/com/mr486/gestojob/service/AnnonceTexteService.java:53`
- Test : `ContenuServiceTest`, `AnnonceMailServiceTest`, `AnnonceTexteServiceTest`

**Interfaces :**
- Produces : `Annonce.lettrePersonnalisee` (`String`, nullable, colonne `lettre_personnalisee TEXT NULL`).
- Produces : `ContenuService.getHtmlContenu(Annonce annonce, String messageDePolitesse) : String` et `ContenuService.getTextContenu(Annonce annonce, String messageDePolitesse) : String`. Elles **remplacent** les signatures `(Titulaire, String poste, Integer typeContenu, String)`. Titulaire, poste et type sont lus sur l'annonce.

- [ ] **Step 1 : adapter les tests existants de `ContenuServiceTest` à la nouvelle signature et ajouter les cas de la lettre**

Un utilitaire de test `annonce(Titulaire t, String poste, Integer type, String lettre)` construit l'`Annonce` avec le builder. Nouveaux tests :

```java
@Test
void html_utiliseLaLettrePersonnalisee_auLieuDuModele() {
    Annonce a = annonce(Titulaire.LAURENT, "Dev Java", 2, "<p>{{POLITESSE}}</p><p>Chez Acme, {{POSTE}}</p>");
    assertThat(contenuService.getHtmlContenu(a, "Madame Durand,"))
            .isEqualTo("<p>Madame Durand,</p><p>Chez Acme, Dev Java</p>");
    verify(modeleEmailService, never()).getContenu(any(), any());
}

@Test
void texte_utiliseLaLettrePersonnalisee() {
    Annonce a = annonce(Titulaire.LAURENT, "Dev", 2, "<p>{{POLITESSE}}</p><p>Lettre Acme</p>");
    assertThat(contenuService.getTextContenu(a, "Monsieur,")).contains("Monsieur,").contains("Lettre Acme");
}

@Test
void lettreBlanche_retombeSurLeModele() {
    Annonce a = annonce(Titulaire.LAURENT, "Dev", 2, "   ");
    contenuService.getHtmlContenu(a, "Madame, Monsieur,");
    verify(modeleEmailService).getContenu(Titulaire.LAURENT, "CONTENU_IA");
}
```

- [ ] **Step 2 : lancer les tests et vérifier l'échec de compilation ou d'assertion**

Run : `./mvnw test -Dtest=ContenuServiceTest`. Attendu : la compilation échoue (signature inexistante).

- [ ] **Step 3 : implémenter**

- Migration :
  ```sql
  --liquibase formatted sql
  --changeset mr486:009-lettre-personnalisee
  ALTER TABLE annonces ADD COLUMN lettre_personnalisee TEXT NULL;
  ```
  Reprendre exactement l'en-tête utilisé par `008-multi-utilisateurs.sql` s'il diffère.
- `Annonce` : `@Column(columnDefinition = "TEXT") private String lettrePersonnalisee;` avec un commentaire `//` d'une ligne. Exclure ce champ du `@ToString` (`@ToString.Exclude`), car il contient la lettre et ne doit pas finir dans les logs.
- `ContenuService.getContent(Annonce, String politesse, boolean isHtml)` : `modeleHtml` vaut `annonce.getLettrePersonnalisee()` si non nul et non blanc, sinon `modeleEmailService.getContenu(...)` comme aujourd'hui. Le reste est inchangé (poste par défaut, échappement HTML, conversion texte).
- Mettre à jour les deux appelants et leurs tests (`AnnonceMailServiceTest` : `when(contenuService.getHtmlContenu(any(Annonce.class), any()))`).

- [ ] **Step 4 : lancer les tests**

Run : `./mvnw test -Dtest='ContenuServiceTest,AnnonceMailServiceTest,AnnonceTexteServiceTest'`. Attendu : PASS.

- [ ] **Step 5 : proposer le commit** `feat(annonces): lettre de motivation personnalisée par annonce`

---

### Task 2 : création d'annonce avec lettre, création de contact qui renvoie l'id

**Files :**
- Modify : `src/main/java/com/mr486/gestojob/service/AnnonceService.java:115-148`
- Modify : `src/main/java/com/mr486/gestojob/service/ContactService.java:65` (type de retour)
- Modify : `src/main/java/com/mr486/gestojob/persistance/ContactRepository.java` (méthode dérivée)
- Test : `AnnonceServiceTest`, `ContactServiceTest`

**Interfaces :**
- Produces : `AnnonceService.creerAvecLettre(AnnonceForm form, String lettreHtml) : Annonce`. Même validation et même règle de statut que `saveForm` (factoriser dans une méthode privée commune qui renvoie l'`Annonce` sauvegardée ; `saveForm` reste `void`). La lettre est nettoyée par `HtmlSanitizer.nettoie`. Si elle ne contient pas `{{POLITESSE}}` après nettoyage, la méthode lève `MetierException("Lettre refusée : la variable {{POLITESSE}} est obligatoire.")`.
- Produces : `ContactService.saveContact(ContactForm, int) : Long`. Renvoie l'id créé, comportement inchangé par ailleurs.
- Produces : `ContactRepository.findAllByEntrepriseIdInAndTitulaire(Collection<Integer> entrepriseIds, Titulaire titulaire) : List<Contact>`.

- [ ] **Step 1 : tests en échec**

```java
@Test
void creerAvecLettre_nettoieEtEnregistreLaLettre() {
    // entreprise 3 et contact 8 existent pour LAURENT (mocks comme dans les tests de saveForm)
    Annonce a = annonceService.creerAvecLettre(formAvecContact(3, 8L),
            "<p>{{POLITESSE}}</p><script>alert(1)</script><p>Acme</p>");
    assertThat(a.getLettrePersonnalisee()).contains("{{POLITESSE}}").contains("Acme").doesNotContain("script");
    assertThat(a.getStatusAnnonce()).isEqualTo(StatutAnnonce.BOITE_ENVOI.getCode());
    assertThat(a.getTitulaire()).isEqualTo(Titulaire.LAURENT);
}

@Test
void creerAvecLettre_sansPolitesse_refuse() {
    assertThatThrownBy(() -> annonceService.creerAvecLettre(formAvecContact(3, 8L), "<p>Bonjour</p>"))
            .isInstanceOf(MetierException.class).hasMessageContaining("{{POLITESSE}}");
    verify(annonceRepository, never()).save(any());
}

@Test
void creerAvecLettre_sansContact_estUneCandidatureSiteDejaEnvoyee() {
    Annonce a = annonceService.creerAvecLettre(formSansContact(3), "<p>{{POLITESSE}}</p>");
    assertThat(a.getStatusAnnonce()).isEqualTo(StatutAnnonce.EN_COURS.getCode());
    assertThat(a.getDateEnvoi()).isNotNull();
}
```

Dans `ContactServiceTest` : `saveContact` renvoie l'id de l'entité sauvegardée (le mock `contactRepository.save` renvoie un `Contact` avec `id = 42L` → résultat `42L`).

Le mock `annonceRepository.save(any())` doit renvoyer son argument (`thenAnswer(inv -> inv.getArgument(0))`).

- [ ] **Step 2 : lancer `./mvnw test -Dtest='AnnonceServiceTest,ContactServiceTest'`**. Attendu : FAIL (méthode absente).
- [ ] **Step 3 : implémenter les trois interfaces ci-dessus.** La méthode privée `save(Annonce)` renvoie maintenant l'`Annonce`.
- [ ] **Step 4 : relancer.** Attendu : PASS, y compris `ContactControllerTest` (`./mvnw test -Dtest='*Contact*,AnnonceServiceTest'`).
- [ ] **Step 5 : proposer le commit** `feat(annonces): création d'annonce avec lettre personnalisée validée`

---

### Task 3 : authentification par jeton (chaîne `/api/**`, fail2ban)

**Files :**
- Create : `src/main/java/com/mr486/gestojob/securite/JetonApiFiltre.java`
- Create : `src/main/java/com/mr486/gestojob/configuration/ApiSecurityConfig.java`
- Modify : `src/main/resources/application.yml` (bloc `gestojob.api.token`, commenté)
- Modify : `dist.env` (variable `GESTOJOB_API_TOKEN=` + commentaire : « jeton de l'API Claude, ≥ 32 octets aléatoires (ex. `openssl rand -base64 48`) ; vide = API désactivée »)
- Test : `src/test/java/com/mr486/gestojob/securite/JetonApiFiltreTest.java`, `src/test/java/com/mr486/gestojob/configuration/SecurityConfigTest.java`

**Interfaces :**
- Produces : `JetonApiFiltre extends OncePerRequestFilter`, constructeur `JetonApiFiltre(String jetonAttendu)`, constante `public static final String EN_TETE = "X-GestoJob-Jeton"`. **Pas de `@Component`** : Spring Boot l'enregistrerait comme filtre servlet global, actif aussi sur les pages. Il est instancié uniquement dans `ApiSecurityConfig`.
- Produces : `ApiSecurityConfig#apiFilterChain(HttpSecurity http, @Value("${gestojob.api.token:}") String jeton) : SecurityFilterChain`, annotée `@Order(1)`, avec `securityMatcher("/api/**")`, `STATELESS`, `csrf` désactivé, `formLogin`/`httpBasic`/`logout` désactivés, `anyRequest().hasRole("TITULAIRE")`, le filtre ajouté `addFilterBefore(..., AnonymousAuthenticationFilter.class)`, et un point d'entrée et un refus d'accès qui renvoient 401/403 en JSON. `SecurityConfig` n'est pas modifié.

Comportement du filtre :
- Le jeton attendu est vide ou blanc, l'en-tête est absent ou faux → réponse 401, `Content-Type: application/json;charset=UTF-8`, corps `{"erreur":"Authentification requise."}`. Une ligne `ECHEC_CONNEXION identifiant="api" adresse=<request.getRemoteAddr()>` est écrite sur `LoggerFactory.getLogger("securite.connexion")` au niveau `warn`. La chaîne n'est pas poursuivie.
- Comparaison en temps constant : `MessageDigest.isEqual(attendu.getBytes(UTF_8), recu.getBytes(UTF_8))`, uniquement si l'attendu n'est pas blanc.
- Jeton valide → `SecurityContextHolder` reçoit un `UsernamePasswordAuthenticationToken.authenticated("api", null, List.of(new SimpleGrantedAuthority("ROLE_TITULAIRE"), new SimpleGrantedAuthority("DONNEES_LAURENT")))`, puis la chaîne continue.

- [ ] **Step 1 : tests unitaires du filtre (`JetonApiFiltreTest`)**, avec le `ListAppender` de `EcouteurEchecConnexionTest` et les adresses RFC 5737 (`@SuppressWarnings("PMD.AvoidUsingHardCodedIP")`) :

```java
@Test void jetonValide_authentifieLaurentTitulaire()      // autorités == {ROLE_TITULAIRE, DONNEES_LAURENT}, chain.doFilter appelé
@Test void jetonFaux_401_etLigneFail2ban()                 // status 401 ; lignes == ["ECHEC_CONNEXION identifiant=\"api\" adresse=203.0.113.7"] ; chain jamais appelé
@Test void enTeteAbsent_401_etLigneFail2ban()
@Test void jetonConfigureVide_etEnTeteVide_401()           // Review Focus 1 : new JetonApiFiltre("") + en-tête ""
@Test void reponse401_estDuJson()                          // contentType commence par "application/json", corps == {"erreur":"Authentification requise."}
@Test void leJetonNApparaitDansAucuneLigne()               // aucune ligne du logger ne contient la valeur envoyée
```

- [ ] **Step 2 : `./mvnw test -Dtest=JetonApiFiltreTest`**. Attendu : FAIL (classe absente).
- [ ] **Step 3 : implémenter le filtre, `ApiSecurityConfig`, `application.yml`, `dist.env`.**
- [ ] **Step 4 : tests d'intégration dans `SecurityConfigTest`**, avec `gestojob.api.token=jeton-de-test-0123456789` ajouté au `@TestPropertySource`. Ces tests utilisent `/api/modele-lettre`, livré en tâche 5. Jusque-là, un jeton valide donne 404, ce qui prouve quand même que l'authentification passe :

```java
@Test void api_sansJeton_401()                         // get("/api/modele-lettre") → 401
@Test void api_sessionFormulaireSansJeton_401()        // Review Focus 2 : .with(user("laurent-test").roles("TITULAIRE")) sans en-tête → 401
@Test void api_postAvecJeton_sansCsrf_pasDe403()       // post("/api/candidatures").header(EN_TETE, jeton) → status != 403 et != 401
@Test void formulaires_postSansCsrf_toujours403()      // non-régression : post("/entreprises/ajout") avec user TITULAIRE sans csrf → 403
```

- [ ] **Step 5 : `./dev-start.sh` puis `./mvnw test -Dtest='JetonApiFiltreTest,SecurityConfigTest'`**. Attendu : PASS.
- [ ] **Step 6 : proposer le commit** `feat(securite): authentification par jeton de l'API, échecs suivis par fail2ban`

---

### Task 4 : `CandidatureApiService` et DTO de l'API

**Files :**
- Create : `src/main/java/com/mr486/gestojob/dto/api/` : records `ModeleLettreApi`, `EntrepriseApi`, `ContactApi`, `CandidatureDemande`, `EntrepriseDemande`, `ContactDemande`, `AnnonceDemande`, `CandidatureReponse`, `ErreurApi`
- Create : `src/main/java/com/mr486/gestojob/service/CandidatureApiService.java`
- Modify : `src/main/java/com/mr486/gestojob/configuration/ApplicationConfiguration.java` (constante `MAX_RESULTATS_RECHERCHE_API = 20`)
- Test : `src/test/java/com/mr486/gestojob/service/CandidatureApiServiceTest.java`

**Interfaces :**
- Consumes : `AnnonceService.creerAvecLettre`, `ContactService.saveContact : Long`, `ContactRepository.findAllByEntrepriseIdInAndTitulaire` (tâche 2) ; `EntrepriseService.save/getEntreprise/setActive`, `ModeleEmailService.getContenu`, `ProfilService.getProfil/posteParDefaut`, `EntrepriseRepository.findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc` (existants).
- Produces (records, champs dans cet ordre) :
  - `ModeleLettreApi(String cle, String html, String posteParDefaut)`
  - `ContactApi(Long id, String nom, String email, Integer formuleDePolitesse)`
  - `EntrepriseApi(Integer id, String nom, String ville, String codePostal, Boolean estActive, List<ContactApi> contacts)`
  - `EntrepriseDemande(Integer id, String nom, String adresse1, String adresse2, String codePostal, String ville)`
  - `ContactDemande(Long id, String email, String nom, Integer formuleDePolitesse)`
  - `AnnonceDemande(@NotNull Integer typeAnnonce, @Size(max = 255) String poste, @Size(max = 255) String reference)`
  - `CandidatureDemande(@NotNull @Valid EntrepriseDemande entreprise, @Valid ContactDemande contact, @NotNull @Valid AnnonceDemande annonce, @NotBlank String lettreHtml)`
  - `CandidatureReponse(Long annonceId, Integer entrepriseId, Long contactId, Integer statut, boolean entrepriseCreee, boolean entrepriseReactivee, boolean contactCree)`
  - `ErreurApi(String erreur)`
- Produces : `CandidatureApiService`
  - `modeleLettre() : ModeleLettreApi` : `cle = CleModele.CONTENU_IA.name()`, html depuis `ModeleEmailService.getContenu(titulaire, "CONTENU_IA")`, poste par défaut du profil.
  - `rechercherEntreprises(String nom) : List<EntrepriseApi>` : `nom` nul ou blanc → `MetierException("Le nom recherché est obligatoire.")`. Résultats limités à `MAX_RESULTATS_RECHERCHE_API`, contacts chargés en **une** requête (`findAllByEntrepriseIdInAndTitulaire`).
  - `@Transactional creer(CandidatureDemande demande) : CandidatureReponse`.

Règles de `creer` :
- Entreprise : si `id` est fourni → `getEntreprise(id)` (une id inconnue ou appartenant à un autre titulaire lève `NoSuchElementException`). Si elle est inactive → `setActive(id, true)`, `entrepriseReactivee = true`. Sans `id` → construire un `EntrepriseForm`, le valider avec `jakarta.validation.Validator` (violations → `MetierException` avec les messages joints par « ; »), puis `EntrepriseService.save`. On obtient `entrepriseCreee = true` et l'entreprise est active.
- Contact : `null` → pas de contact (candidature « site »). Avec `id` → `contactService.getContact(id)` ; si `contact.getEntrepriseId()` diffère de l'entreprise → `NoSuchElementException`. Sans `id` → `ContactForm` validé de la même façon, `saveContact(form, entrepriseId)`, `contactCree = true`.
- Annonce : `AnnonceForm(entrepriseId, contenuId = TypeContenu.IA.getCode(), contactId, typeAnnonce, poste, reference)` → `annonceService.creerAvecLettre(form, demande.lettreHtml())`.
- Log `info` : `candidature API créée : annonce {} (entreprise {}, créée={}, réactivée={})`. Ni la lettre ni le poste ne sont journalisés.

- [ ] **Step 1 : tests en échec (`CandidatureApiServiceTest`, Mockito, `UtilisateurCourant` mocké → LAURENT)**

```java
@Test void modeleLettre_renvoieLeModeleIaEtLePosteParDefaut()
@Test void recherche_nomBlanc_refusee()                       // Review Focus 4 : "  " → MetierException, repository jamais appelé
@Test void recherche_chargeLesContactsEnUneRequete()          // 2 entreprises → findAllByEntrepriseIdInAndTitulaire appelé une fois
@Test void creer_entrepriseExistanteActive_contactExistant()   // entrepriseCreee=false, reactivee=false, contactCree=false, setActive jamais appelé
@Test void creer_entrepriseInactive_estReactivee()            // setActive(12, true), entrepriseReactivee=true
@Test void creer_nouvelleEntreprise_etNouveauContact()        // save(form) → 31, saveContact(form, 31) → 77L ; reponse.entrepriseId()==31, contactId()==77L
@Test void creer_sansContact_annonceSite()                    // AnnonceForm.contactId == null
@Test void creer_contactDUneAutreEntreprise_404()             // Review Focus 3 : contact 8 rattaché à 99, entreprise 12 → NoSuchElementException, creerAvecLettre jamais appelé
@Test void creer_nouvelleEntrepriseSansVille_refusee()        // MetierException contenant "La ville est obligatoire."
@Test void creer_typeContenuToujoursIa()                      // ArgumentCaptor<AnnonceForm> → getContenuId()==2
```

- [ ] **Step 2 : `./mvnw test -Dtest=CandidatureApiServiceTest`**. Attendu : FAIL.
- [ ] **Step 3 : implémenter les records et le service.** Javadoc française sur chaque record et chaque méthode publique.
- [ ] **Step 4 : relancer.** Attendu : PASS.
- [ ] **Step 5 : proposer le commit** `feat(api): service de création de candidature et DTO de l'API`

---

### Task 5 : contrôleur REST et erreurs JSON

**Files :**
- Create : `src/main/java/com/mr486/gestojob/controller/api/CandidatureApiController.java`
- Create : `src/main/java/com/mr486/gestojob/controller/api/ApiErreurConseil.java`
- Test : `src/test/java/com/mr486/gestojob/controller/api/CandidatureApiControllerTest.java`, `.../ApiErreurConseilTest.java`

**Interfaces :**
- Consumes : `CandidatureApiService` et les DTO de la tâche 4.
- Produces : `@RestController @RequestMapping("/api")`
  - `@GetMapping("/modele-lettre") ModeleLettreApi modeleLettre()`
  - `@GetMapping("/entreprises") List<EntrepriseApi> entreprises(@RequestParam(required = false) String nom)`
  - `@PostMapping("/candidatures") ResponseEntity<CandidatureReponse> creer(@Valid @RequestBody CandidatureDemande demande)` → 201
- Produces : `@RestControllerAdvice(basePackageClasses = CandidatureApiController.class) @Order(Ordered.HIGHEST_PRECEDENCE)`, pour passer avant `ErreurConseil` :
  - `MetierException` → 400 `ErreurApi(message)`
  - `MethodArgumentNotValidException` → 400, messages des champs joints par « ; »
  - `HttpMessageNotReadableException` → 400 `ErreurApi("Corps de requête JSON illisible.")`
  - `NoSuchElementException` → 404 `ErreurApi("Ressource introuvable.")`

- [ ] **Step 1 : tests en échec.** Contrôleur appelé directement, service mocké : `creer` renvoie 201 et le corps du service ; les deux GET délèguent. Conseil : chaque gestionnaire renvoie le bon statut et le bon `ErreurApi`. Ajouter à `SecurityConfigTest` (Review Focus 5) :

```java
@Test void api_jsonIllisible_400Json()     // post avec jeton, contentType JSON, corps "{pas du json" → 400, contentType JSON, $.erreur présent
@Test void api_annonceAbsente_400Json()    // corps {"entreprise":{"id":1},"lettreHtml":"<p>{{POLITESSE}}</p>"} → 400 JSON
@Test void api_modeleLettre_avecJeton_200()
```

- [ ] **Step 2 : lancer les tests.** Attendu : FAIL.
- [ ] **Step 3 : implémenter.**
- [ ] **Step 4 : `./mvnw test -Dtest='CandidatureApiControllerTest,ApiErreurConseilTest,SecurityConfigTest'`**. Attendu : PASS.
- [ ] **Step 5 : proposer le commit** `feat(api): points d'entrée REST /api et erreurs JSON`

---

### Task 6 : documentation et vérification complète

**Files :**
- Modify : `README.md` (section « API Claude » : rôle, en-tête, les 3 points d'entrée ; section Déploiement : ajouter `GESTOJOB_API_TOKEN` au `.env` serveur avant `./maj.sh` et vérifier que Traefik laisse passer `/api/**`)
- Modify : `CLAUDE.md` (sous-section Architecture : chaîne `/api/**`, `JetonApiFiltre`, `lettre_personnalisee` prioritaire sur le modèle ; ajouter `GESTOJOB_API_TOKEN` à la section Configuration)

- [ ] **Step 1 : rédiger les deux sections.**
- [ ] **Step 2 : `./dev-start.sh && ./mvnw clean verify`.** Attendu : BUILD SUCCESS, tous les tests passent. Relever les violations Checkstyle, PMD et SpotBugs dans le rapport, puis corriger celles introduites par la branche (objectif : 0, comme le reste du projet).
- [ ] **Step 3 : proposer le commit** `docs: documente l'API Claude et son jeton`

---

### Task 7 : skill `gestion-annonce`

**Files :**
- Create : `~/.claude/skills/gestion-annonce/SKILL.md`
- Modify : `~/.claude/skills/README.md` et `~/.claude/skills/description-skills.md` (une entrée chacun, dans le style existant)

**Interfaces :**
- Consumes : l'API des tâches 3 à 5.
- Configuration dans le SKILL.md : `URL_GESTOJOB` (URL publique de prod, **à demander à Laurent**, non secrète). Le jeton vient de la variable de porte-clés `API_GESTOJOB_MDP` (entrée KeePassXC, que Laurent crée), chargée **dans le même appel** que le `curl`, selon `sas-securise` :
  ```bash
  set -a; . "$HOME/F0rtkn0x/echange-ai/porte-cles.env"; set +a
  curl -sS -H "X-GestoJob-Jeton: ${API_GESTOJOB_MDP}" "$URL_GESTOJOB/api/modele-lettre"
  ```

Contenu exigé du SKILL.md :
- Frontmatter : `name: gestion-annonce`. Description en français avec ces déclencheurs : « analyse cette annonce », « postule à cette offre », « personnalise ma lettre », « enregistre cette candidature », une offre d'emploi collée.
- Déroulé en 7 étapes, repris de la section 4 de la spec : analyse → modèle → doublons → lettre → **relecture obligatoire avant tout POST** → POST → au premier 401, arrêt sans réessayer.
- Règles de rédaction (option A) : réécrire seulement l'accroche (1er paragraphe après `{{POLITESSE}}`) et le paragraphe « Vous rejoindre » ; ordre des technologies du socle technique modifiable, sans ajout ni retrait ; tout le reste identique au caractère près ; `{{POLITESSE}}` conservé aux deux emplacements ; aucune information sur Laurent absente de la lettre de base.
- Présentation à Laurent : un tableau des champs extraits, le choix entreprise/contact (réutilisée, réactivée ou créée), les deux paragraphes réécrits avant/après, et l'ordre des technologies s'il a changé.
- Construction du JSON par `jq -n --arg ...`, jamais par concaténation de chaînes, pour un échappement sûr de la lettre.
- Après le POST : ids, statut ; « envoi depuis /file » si statut 1, « candidature site déjà marquée envoyée » si statut 2.

- [ ] **Step 1 : rédiger le SKILL.md** (s'appuyer sur `superpowers:writing-skills`).
- [ ] **Step 2 : test à blanc.** Avec l'app locale (`./mvnw spring-boot:run`, `GESTOJOB_API_TOKEN` dans le `.env` local) et `URL_GESTOJOB=http://localhost:<port>`, coller une offre fictive. Vérifier : relecture affichée avant écriture, annonce visible dans `/file` avec la lettre (`/contenu/{id}`), et arrêt au premier 401 avec un mauvais jeton.
- [ ] **Step 3 : demander à Laurent** avant `maj-skills.sh` (commit et push du dépôt `claude-skills`).

---

### Task 8 : mise en production (gestes de Laurent, l'assistant guide)

- [ ] Créer l'entrée KeePassXC du jeton (`openssl rand -base64 48`), l'ajouter à `porte-cles.conf` sous `API_GESTOJOB_MDP`, puis la reporter dans le `.env` du serveur (`GESTOJOB_API_TOKEN=`).
- [ ] Fusionner la PR, puis lancer `./maj.sh` sur le serveur. La migration 009 s'applique au démarrage.
- [ ] Test de fumée depuis le poste : `GET /api/modele-lettre` → 200. Un mauvais jeton → 401, et la ligne `ECHEC_CONNEXION identifiant="api"` apparaît dans `connexions.log`. Un seul essai raté, pour ne pas déclencher le bannissement.
