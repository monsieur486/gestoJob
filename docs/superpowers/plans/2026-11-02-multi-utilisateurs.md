# GestoJob multi-utilisateurs — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** quatre comptes (laurent, renee, ft01, ft02). Les données sont cloisonnées par titulaire (LAURENT / RENEE), les comptes ft sont en lecture seule, chaque titulaire a son profil, ses modèles et son expéditeur Gmail.

**Architecture :** un enum `Titulaire` est stocké dans une colonne `titulaire` sur les quatre tables métier. Un composant `UtilisateurCourant` déduit ce titulaire de l'authentification (autorité `DONNEES_<TITULAIRE>`). Les services d'entrée (entreprises, contacts, annonces, paramètres) filtrent toutes leurs requêtes par ce titulaire. La chaîne de contenu (modèles, libellés, mail) reçoit le titulaire explicitement, celui de l'annonce. La lecture seule est imposée dans `SecurityConfig` (rôle `LECTEUR`) et seulement *reflétée* dans les vues.

**Tech Stack :** Spring Boot 4.0.2, Spring Security, Thymeleaf + `thymeleaf-extras-springsecurity6`, Spring Data JPA, Liquibase (SQL), Jakarta Mail (`JavaMailSenderImpl`), JUnit 5 + Mockito.

**Spec :** `docs/superpowers/specs/2026-11-02-multi-utilisateurs-design.md`

## Global Constraints

- Tout en français : identifiants, Javadoc (style maison), messages, UI.
- Le schéma passe **uniquement** par Liquibase : nouveau fichier `src/main/resources/db/changelog/008-multi-utilisateurs.sql`, inclus dans `master.xml`. `ddl-auto: validate` reste en place.
- Les secrets vont dans `.env`, et `dist.env` est mis à jour. `GESTOJOB_ADMIN_*` et `MAIL_USERNAME/PASSWORD` disparaissent.
- `application.yml` **et** `application-docker.yml` reçoivent les mêmes modifications (les deux fichiers sont dupliqués).
- `MAIL_LAURENT_*` est obligatoire. `MAIL_RENEE_*` est facultatif (valeur par défaut vide).
- Les codes `TypeContenu` (1 microservices, 2 IA, sinon général) et les clés `CleModele` restent inchangés.
- Un id appartenant à l'autre titulaire se comporte exactement comme un id inexistant.
- Null-safety : aucun unboxing ni `Map.get` non protégé.
- Chaque méthode publique nouvelle ou modifiée a son test. JaCoCo vise 80 % (indicatif).
- Checkstyle, PMD et SpotBugs : 0 violation à la fin de chaque tâche (`./mvnw verify` avec Postgres démarré via `./dev-start.sh`).
- Branche `feat/multi-utilisateurs`. Commits en français (Conventional Commits). **Pas de commit sans la relecture de Laurent** : chaque étape « Commit » consiste à présenter le diff et à attendre son accord.

## Review Focus

1. **Accès par URL directe à un id de l'autre titulaire** (`/entreprises/{id}`, `/contenu/{id}`, `POST /annonces/{id}/positif`…) : il faut la même réponse qu'un id inconnu, sans aucune fuite ni modification. Tests dans les tâches 5 et 6 (`…_idAutreTitulaire_…`).
2. **Bouton « Envoyer toute la file » cliqué par renée** : seules ses annonces partent, depuis son adresse. Les annonces en file de laurent ne bougent pas. Test dans la tâche 7 (`sendEmailForPendingAnnonces_nEnvoieQueLesAnnoncesDuTitulaireCourant`).
3. **Lecteur qui forge un POST** (CSRF valide, compte ft01) : réponse 403 et rien n'est modifié. Il en va de même pour le GET des formulaires de création. Tests dans la tâche 2.
4. **Renée envoie avant que son Gmail soit configuré** : message clair, statut inchangé, l'application ne plante pas. Test dans la tâche 7.
5. **Même nom d'entreprise ou même email de contact chez les deux titulaires** : la saisie est acceptée, car l'unicité est vérifiée par titulaire. Test dans la tâche 5.

---

### Task 1 : Titulaire, migration 008, colonnes sur les entités

**Files :**
- Create : `src/main/java/com/mr486/gestojob/model/Titulaire.java`
- Create : `src/main/resources/db/changelog/008-multi-utilisateurs.sql`
- Modify : `src/main/resources/db/changelog/master.xml`
- Modify : `model/Entreprise.java`, `model/Contact.java`, `model/Annonce.java`, `model/ModeleEmail.java` (champ `titulaire`)
- Test : `src/test/java/com/mr486/gestojob/model/TitulaireTest.java`

**Interfaces :**
- Produces : `enum Titulaire { LAURENT, RENEE }` avec `String getAffichage()` (« Laurent », « Renée »). Chaque entité porte `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Titulaire titulaire;`.

- [ ] **Step 1 : créer la branche** `git switch -c feat/multi-utilisateurs`
- [ ] **Step 2 : test qui échoue** — `TitulaireTest.getAffichage_renvoieLeNomAccentue()` : `assertEquals("Renée", Titulaire.RENEE.getAffichage())` et `assertEquals("Laurent", Titulaire.LAURENT.getAffichage())`.
- [ ] **Step 3 :** `./mvnw test -Dtest=TitulaireTest` → échec de compilation.
- [ ] **Step 4 : écrire `Titulaire`** et ajouter le champ aux quatre entités.
- [ ] **Step 5 : écrire `008-multi-utilisateurs.sql`.** Le contenu est fixé par la spec :

```sql
-- Rattachement de chaque donnée à un titulaire ; l'existant appartient à LAURENT.
ALTER TABLE entreprises  ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE contacts     ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE annonces     ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE modele_email ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE entreprises  ALTER COLUMN titulaire DROP DEFAULT;
ALTER TABLE contacts     ALTER COLUMN titulaire DROP DEFAULT;
ALTER TABLE annonces     ALTER COLUMN titulaire DROP DEFAULT;
ALTER TABLE modele_email ALTER COLUMN titulaire DROP DEFAULT;
CREATE INDEX idx_entreprises_titulaire ON entreprises (titulaire);
CREATE INDEX idx_contacts_titulaire    ON contacts (titulaire);
CREATE INDEX idx_annonces_titulaire    ON annonces (titulaire);

-- Un modèle par clé ET par titulaire.
ALTER TABLE modele_email DROP CONSTRAINT uq_modele_email_cle;
ALTER TABLE modele_email ADD CONSTRAINT uq_modele_email_titulaire_cle UNIQUE (titulaire, cle);

-- Profil : nom du candidat + libellé et poste par défaut des trois types de lettre.
CREATE TABLE profil (
    titulaire              VARCHAR(16)  NOT NULL,
    candidat_nom           VARCHAR(255) NOT NULL,
    libelle_general        VARCHAR(64)  NOT NULL,
    poste_general          VARCHAR(255) NOT NULL,
    libelle_microservices  VARCHAR(64)  NOT NULL,
    poste_microservices    VARCHAR(255) NOT NULL,
    libelle_ia             VARCHAR(64)  NOT NULL,
    poste_ia               VARCHAR(255) NOT NULL,
    CONSTRAINT pk_profil PRIMARY KEY (titulaire)
);
INSERT INTO profil VALUES
 ('LAURENT', 'Laurent Touret',
  'Général', 'de développeur Java',
  'Microservices', 'de développeur Java orienté microservices',
  'IA agentique', 'de développeur Java back-end orienté IA agentique'),
 ('RENEE', 'Renée',
  'Lettre 1', 'correspondant à mon profil',
  'Lettre 2', 'correspondant à mon profil',
  'Lettre 3', 'correspondant à mon profil');
```

  Les colonnes `*_microservices` et `*_ia` gardent ces noms parce qu'elles suivent les clés `CONTENU_*`. Seul l'affichage change.
- [ ] **Step 6 :** `./mvnw test -Dtest=TitulaireTest` passe. Puis `./dev-start.sh && ./mvnw test -Dtest=GestoJobApplicationTests` doit passer : Liquibase applique la migration et `validate` accepte les entités.
- [ ] **Step 7 : Commit** `feat(donnees): rattache chaque donnée à un titulaire (migration 008)`

---

### Task 2 : quatre comptes, rôles, lecture seule côté serveur, accueil privé

**Files :**
- Create : `src/main/java/com/mr486/gestojob/securite/UtilisateurCourant.java`
- Modify : `configuration/SecurityConfig.java`, `application.yml`, `application-docker.yml`, `dist.env`
- Test : `securite/UtilisateurCourantTest.java`, `configuration/SecurityConfigTest.java` (à réécrire)

**Interfaces :**
- Consumes : `Titulaire` (tâche 1).
- Produces : `@Component UtilisateurCourant` avec `Titulaire titulaire()`, qui lève `IllegalStateException` sans authentification ou sans autorité `DONNEES_*`, et `boolean estLecteur()`. Les autorités accordées sont : laurent → `ROLE_TITULAIRE`, `DONNEES_LAURENT` ; renee → `ROLE_TITULAIRE`, `DONNEES_RENEE` ; ft01 → `ROLE_LECTEUR`, `DONNEES_LAURENT` ; ft02 → `ROLE_LECTEUR`, `DONNEES_RENEE`. Le lien entre compte et titulaire est écrit dans le code.
- Propriétés : `gestojob.comptes.{laurent,renee,ft01,ft02}.{username,password}`, issues de `GESTOJOB_{LAURENT,RENEE,FT01,FT02}_{USERNAME,PASSWORD}`, sans valeur par défaut. Le bloc `gestojob.admin` est supprimé.

- [ ] **Step 1 : tests qui échouent.**
  - `UtilisateurCourantTest` : on place un `TestingAuthenticationToken` dans le `SecurityContextHolder`, puis on vérifie `titulaire_renvoieRenee_pourAutoriteDonneesRenee`, `estLecteur_vrai_pourRoleLecteur` et `titulaire_leveIllegalState_sansAuthentification`. Le contexte est vidé dans `@AfterEach`.
  - `SecurityConfigTest` : profil de test avec les identifiants fournis par `@TestPropertySource`. Tests :
    - `accueil_redirigeVersLogin_siNonAuthentifie` : `get("/")` → `redirectedUrl` se termine par `/login` ;
    - `connexion_reussit_pourChacunDesQuatreComptes` (paramétré) ;
    - `lecteur_postRefuse_403` : `post("/entreprises/ajout").with(user("ft01").roles("LECTEUR")).with(csrf())` → 403 ;
    - `lecteur_formulaireCreationRefuse_403` : GET `/entreprises/ajout` et `/entreprises/1/contacts/ajout` → 403 ;
    - `lecteur_consultationAutorisee` : GET `/login` → 200, et GET `/annonces` n'est pas un 403 ;
    - `titulaire_postAutorise` : un POST avec `roles("TITULAIRE")` n'est pas un 403 ;
    - `lecteur_peutSeDeconnecter` : POST `/logout` avec csrf → redirection.
- [ ] **Step 2 :** `./mvnw test -Dtest='UtilisateurCourantTest,SecurityConfigTest'` → échec.
- [ ] **Step 3 : implémenter.**
  - `SecurityConfig.userDetailsService` construit les quatre comptes.
  - Règles, dans cet ordre :
    1. `permitAll` sur `/login`, `/css/**`, `/js/**`, `/favicon.ico` (`/` en est retiré) ;
    2. `hasRole("TITULAIRE")` sur `/entreprises/ajout` et `/entreprises/*/contacts/ajout` ;
    3. `requestMatchers(HttpMethod.GET, "/**").authenticated()` ;
    4. `anyRequest().hasRole("TITULAIRE")`.
  - `/logout` et `POST /login` sont traités par leurs filtres avant l'autorisation : les tests le vérifient.
  - Mettre à jour la Javadoc de classe, les deux YAML (commentés) et `dist.env`, avec les quatre paires de comptes et des valeurs d'exemple.
- [ ] **Step 4 :** relancer les tests de l'étape 2 → ils passent.
- [ ] **Step 5 : Commit** `feat(securite): quatre comptes, rôles titulaire/lecteur et lecture seule côté serveur`

---

### Task 3 : profil par titulaire et modèles par défaut par titulaire

**Files :**
- Create : `model/Profil.java`, `persistance/ProfilRepository.java`, `service/ProfilService.java`, `dto/ProfilForm.java`
- Create : `src/main/resources/modeles/defaut/RENEE/{CONTENU_GENERAL,CONTENU_MICROSERVICES,CONTENU_IA}.html`, `…/RENEE/{LIBELLE_REFERENCE,LIBELLE_SPONTANEE}.txt`
- Move : `src/main/resources/modeles/defaut/*` → `src/main/resources/modeles/defaut/LAURENT/` (`git mv`)
- Modify : `model/CleModele.java`, `configuration/ApplicationConfiguration.java`
- Test : `service/ProfilServiceTest.java`, `model/CleModeleTest.java`, `dto/ProfilFormTest.java`

**Interfaces :**
- Consumes : `Titulaire`, `UtilisateurCourant.titulaire()`.
- Produces :
  - `Profil` (entité de la table `profil`, `@Id @Enumerated(STRING) Titulaire titulaire`, avec les champs des colonnes de la tâche 1) ;
  - `ProfilService.getProfil(Titulaire) : Profil` (lève `MetierException` si la ligne est absente) ;
  - `ProfilService.getProfilCourant() : Profil` ;
  - `ProfilService.mettreAJour(ProfilForm) : void` (titulaire courant) ;
  - `ProfilService.libelleContenu(Profil, CleModele) : String` et `ProfilService.posteParDefaut(Profil, CleModele) : String` (pour une clé `CONTENU_*`, sinon `IllegalArgumentException`) ;
  - `CleModele.getCheminRessource(Titulaire) : String` → `"modeles/defaut/" + titulaire.name() + "/" + name() + (html ? ".html" : ".txt")`. `CleModele.getPosteParDefaut()` est **supprimée**.
  - Dans `ApplicationConfiguration`, `CANDIDAT_NOM`, `DEFAULT_EMAIL_FROM` et `POSTE_DEFAUT_*` sont **supprimés**. Les civilités et `SALUTATION_GENERIQUE` restent.
- `ProfilForm` : champs `candidatNom` et `libelle*` (`@NotBlank`, longueur max 64 pour les libellés), `poste*` (`@NotBlank`, max 255).

- [ ] **Step 1 : tests qui échouent.**
  - `CleModeleTest.getCheminRessource_dependDuTitulaire` : `CONTENU_IA.getCheminRessource(RENEE)` vaut `"modeles/defaut/RENEE/CONTENU_IA.html"`, et `LIBELLE_SPONTANEE.getCheminRessource(LAURENT)` vaut `"modeles/defaut/LAURENT/LIBELLE_SPONTANEE.txt"`.
  - `CleModeleTest.ressourcesParDefaut_existentPourChaqueTitulaireEtChaqueCle` : boucle sur `Titulaire × CleModele` avec `getClass().getClassLoader().getResource(...)` non nul.
  - `CleModeleTest.ressourcesRenee_neContiennentAucunTexteDeLaurent` : aucune ressource RENEE ne contient « Laurent », « Java » ni « OpenClassrooms ».
  - `ProfilServiceTest` : `posteParDefaut_microservices_renvoieLeChampMicroservices`, `libelleContenu_general`, `getProfil_absent_leveMetierException`, `mettreAJour_enregistreSurLeTitulaireCourant` (`UtilisateurCourant` et repository mockés ; `ArgumentCaptor` vérifie titulaire = RENEE et `candidatNom` mis à jour).
- [ ] **Step 2 :** `./mvnw test -Dtest='CleModeleTest,ProfilServiceTest,ProfilFormTest'` → échec.
- [ ] **Step 3 : implémenter.**
  - Ressources de RENEE : trame courte et neutre qui contient `{{POLITESSE}}` et `{{POSTE}}`, au même format HTML que les fichiers de laurent.
  - Libellés : `Réf [{{REFERENCE}}] {{NOM}} - candidature au poste {{POSTE}}` et `{{NOM}} - Candidature spontanée`.
- [ ] **Step 4 :** relancer les tests de l'étape 2 → ils passent. La compilation casse ailleurs (usages de `getPosteParDefaut`, `CANDIDAT_NOM`, `getCheminRessource()`) : c'est attendu, la tâche 4 répare. Les tâches 3 et 4 sont donc commitées ensemble à la fin de la tâche 4.

---

### Task 4 : modèles d'email, contenu et libellés selon le titulaire

**Files :**
- Modify : `service/ModeleEmailService.java`, `service/ContenuService.java`, `service/LibelleService.java`, `persistance/ModeleEmailRepository.java`, `configuration/ModeleEmailInitializer.java`, `dto/ModeleEmailVue.java` (si besoin)
- Test : `service/ModeleEmailServiceTest.java`, `service/ContenuServiceTest.java`, `service/LibelleServiceTest.java`

**Interfaces :**
- Consumes : `ProfilService` (tâche 3), `UtilisateurCourant`.
- Produces :
  - `ModeleEmailRepository.findByTitulaireAndCle(Titulaire, String) : Optional<ModeleEmail>` (remplace `findByCle`) ;
  - `ModeleEmailService.initialiser()` : boucle sur tous les titulaires et toutes les clés, et crée les modèles absents à partir de `getCheminRessource(titulaire)` ;
  - `getContenu(Titulaire, String cle) : String` ; cache `ConcurrentMap` indexé par `titulaire.name() + ":" + cle` ;
  - `listerModeles() : List<ModeleEmailVue>` : titulaire courant, et pour les clés `CONTENU_*`, `libelleUi` vient du profil ;
  - `mettreAJour(String cle, String contenuBrut)` et `reinitialiser(String cle)` : titulaire courant ;
  - `ContenuService.getHtmlContenu(Titulaire, String poste, Integer typeContenu, String politesse)` et `getTextContenu(...)` (même ordre) : le poste par défaut vient de `ProfilService.posteParDefaut(getProfil(titulaire), cle)` ;
  - `LibelleService.construitLibelle(Annonce)` : modèle et `{{NOM}}` pris dans le profil de `annonce.getTitulaire()`.

- [ ] **Step 1 : adapter et ajouter les tests (qui échouent).**
  - `ModeleEmailServiceTest.initialiser_creeLesModelesAbsentsPourChaqueTitulaire` : repository vide, puis `save` appelé `2 × CleModele.values().length` fois.
  - `getContenu_isoleLeCacheParTitulaire` : LAURENT et RENEE renvoient des contenus différents pour la même clé.
  - `reinitialiser_rechargeLeDefautDuTitulaireCourant` : RENEE → contenu de la ressource RENEE.
  - `ContenuServiceTest.posteVide_utiliseLePosteParDefautDuProfil` : RENEE, typeContenu 1 et poste `""` → le HTML contient « correspondant à mon profil ».
  - `LibelleServiceTest.construitLibelle_utiliseLeNomDuProfilDeLAnnonce` : annonce RENEE, nom « Renée » → le libellé commence par « Renée - ».
  - Les tests existants sont adaptés aux nouvelles signatures.
- [ ] **Step 2 :** lancer ces tests → échec.
- [ ] **Step 3 : implémenter.** Le contrôleur et la vue Paramètres continuent de compiler : ils appellent les méthodes sans titulaire, qui prennent le titulaire courant.
- [ ] **Step 4 :** `./mvnw test` : les tests de modèles, contenu, libellés et profil passent. Ce qui reste cassé relève des appelants des tâches 6 et 7 : `AnnonceMailService` et `AnnonceTexteService` appellent `getHtmlContenu` avec `annonce.getTitulaire()` en premier argument. Faire cette adaptation minimale ici pour que tout compile.
- [ ] **Step 5 : Commit** (tâches 3 et 4) `feat(profil): profil et modèles d'email par titulaire`

---

### Task 5 : cloisonnement des entreprises et des contacts

**Files :**
- Modify : `persistance/EntrepriseRepository.java`, `persistance/ContactRepository.java`, `service/EntrepriseService.java`, `service/EntrepriseRechercheService.java`, `service/ContactService.java`
- Test : `service/EntrepriseServiceTest.java`, `service/EntrepriseRechercheServiceTest.java`, `service/ContactServiceTest.java`

**Interfaces :**
- Consumes : `UtilisateurCourant.titulaire()`.
- Produces (les anciennes méthodes non filtrées sont **supprimées** des repositories, pour qu'aucun appel non filtré ne subsiste) :
  - `EntrepriseRepository` : `findByIdAndTitulaire(Integer, Titulaire)`, `findAllByTitulaireOrderByNomAsc(Titulaire, Pageable)`, `findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc(Titulaire, String, Pageable)`, `findAllByTitulaireAndEstActiveTrueOrderByNomAsc(Titulaire, Pageable)`, `existsByTitulaireAndNomIgnoreCase(Titulaire, String)`, `existsByIdAndTitulaire(Integer, Titulaire)`, `countByTitulaire(Titulaire)`, `findAllByIdInAndTitulaire(Collection<Integer>, Titulaire)`.
  - `ContactRepository` : `findByIdAndTitulaire(Long, Titulaire)`, `findAllByEntrepriseIdAndTitulaire(Integer, Titulaire)`, `existsByTitulaireAndEntrepriseIdAndEmailIgnoreCase(Titulaire, Integer, String)`, `findAllByIdInAndTitulaire(Collection<Long>, Titulaire)`.
  - Les signatures publiques des services ne changent pas. `save` et `saveContact` posent `titulaire` sur l'entité. `deleteContact` charge d'abord le contact par id + titulaire (même exception qu'un id inconnu), puis le supprime.
  - `ContactService.saveContact` vérifie que l'entreprise cible appartient au titulaire courant.

- [ ] **Step 1 : tests qui échouent** (`UtilisateurCourant` mocké pour renvoyer RENEE).
  - `EntrepriseServiceTest.getEntreprise_idAutreTitulaire_seComporteCommeInconnu` : `findByIdAndTitulaire(5, RENEE)` est vide → même exception qu'aujourd'hui pour un id inconnu.
  - `save_poseLeTitulaireCourant` (captor).
  - `save_nomDejaPrisParLAutreTitulaire_accepte` : `existsByTitulaireAndNomIgnoreCase(RENEE, "Acme")` est faux → enregistré.
  - `countAllEntreprises_compteLeTitulaireCourant`.
  - `setActive_idAutreTitulaire_neModifieRien` : `save` n'est jamais appelé.
  - `EntrepriseRechercheServiceTest` : les trois pages appellent la variante `…ByTitulaire…` avec RENEE.
  - `ContactServiceTest.deleteContact_idAutreTitulaire_neSupprimeRien`, `saveContact_entrepriseAutreTitulaire_refuse`, `saveContact_poseLeTitulaireCourant`.
- [ ] **Step 2 :** lancer ces trois classes de test → échec.
- [ ] **Step 3 : implémenter.**
- [ ] **Step 4 :** `./mvnw test` → tout passe (contrôleurs compris : ils ne voient pas le changement).
- [ ] **Step 5 : Commit** `feat(donnees): cloisonne entreprises et contacts par titulaire`

---

### Task 6 : cloisonnement des annonces (listes, recherche, statuts, texte, accueil)

**Files :**
- Modify : `persistance/AnnonceRepository.java`, `service/AnnonceService.java`, `service/AnnonceStatutService.java`, `service/AnnonceTexteService.java`, `service/AnnonceListeMapper.java` (si les lookups groupés changent de nom)
- Test : `service/AnnonceServiceTest.java`, `service/AnnonceStatutServiceTest.java`, `service/AnnonceTexteServiceTest.java`, `controller/ContenuControllerTest.java`, `controller/HomePageControllerTest.java`

**Interfaces :**
- Consumes : `UtilisateurCourant`, les lookups groupés filtrés de la tâche 5.
- Produces :
  - `AnnonceRepository` : chaque méthode prend un paramètre `Titulaire titulaire` en premier. Ce sont `findByIdAndTitulaire`, `existsByContactIdAndTitulaire`, `findAllByTitulaireAndStatusAnnonce`, `findAllByTitulaireAndEntrepriseIdOrderByStatusAnnonceAsc`, `findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc`, `countByTitulaire`, puis les `@Query` `findAllOrderByDateEnvoiDesc`, `findAllByStatusAnnonceNotOrderByDateEnvoiDesc` et `search`. Ces requêtes reçoivent `a.titulaire = :titulaire`, et `search` reçoit aussi un filtre sur les jointures `e` et `c`. Les deux `@Modifying update…` reçoivent `and a.titulaire = :titulaire` et renvoient `int` (nombre de lignes).
  - `AnnonceStatutService.setRefus` et `setAccepte` (ainsi que `setDepasse`) : si 0 ligne est modifiée, ils lèvent la même exception qu'un id inconnu.
  - `AnnonceService.saveForm` pose le titulaire courant et vérifie que l'entreprise et le contact éventuel appartiennent au titulaire courant. `deleteAnnonce` charge d'abord par id + titulaire.
  - Les signatures publiques des services ne changent pas.

- [ ] **Step 1 : tests qui échouent** (titulaire courant RENEE).
  - `AnnonceServiceTest.getAnnonce_idAutreTitulaire_seComporteCommeInconnu`.
  - `deleteAnnonce_idAutreTitulaire_neSupprimeRien`.
  - `searchAnnoncesPage_filtreParTitulaire` : vérifie l'appel à `search(RENEE, …)`.
  - `countAnnonces_compteLeTitulaireCourant`, `getAllPositifListePage_filtreParTitulaire`.
  - `saveForm_contactAutreTitulaire_refuse`, `saveForm_poseLeTitulaireCourant`.
  - `AnnonceStatutServiceTest.setAccepte_idAutreTitulaire_leveExceptionEtNeModifieRien` (update renvoie 0).
  - `AnnonceTexteServiceTest.getAnnonceTxtContenuById_idAutreTitulaire_seComporteCommeInconnu`. C'est ce qui protège `/contenu/{id}`.
- [ ] **Step 2 :** lancer ces classes → échec.
- [ ] **Step 3 : implémenter.**
- [ ] **Step 4 :** `./mvnw test` → tout passe.
- [ ] **Step 5 : Commit** `feat(donnees): cloisonne les annonces par titulaire`

---

### Task 7 : envoi depuis l'adresse Gmail du titulaire

**Files :**
- Create : `configuration/MailConfiguration.java`
- Modify : `tools/Mailer.java`, `tools/MailTools.java`, `service/AnnonceMailService.java`, `controller/FileAttenteController.java` (affichage de l'erreur), `application.yml`, `application-docker.yml`, `dist.env`
- Test : `tools/MailToolsTest.java`, `service/AnnonceMailServiceTest.java`, `controller/FileAttenteControllerTest.java`, `configuration/MailConfigurationTest.java`

**Interfaces :**
- Consumes : `Titulaire`, `ProfilService.getProfil(Titulaire)`, `UtilisateurCourant`, `AnnonceRepository.findAllByTitulaireAndStatusAnnonce` (tâche 6).
- Produces :
  - Propriétés `gestojob.mail.laurent.{username,password}` ← `MAIL_LAURENT_*` (obligatoires) et `gestojob.mail.renee.{username,password}` ← `${MAIL_RENEE_USERNAME:}` / `${MAIL_RENEE_PASSWORD:}`. Hôte, port et `properties` restent sous `spring.mail` (sans username ni password), et `MailConfiguration` les relit.
  - `MailConfiguration` : bean `Map<Titulaire, JavaMailSenderImpl> expediteursMail`, qui ne contient **que** les titulaires dont le username n'est pas vide.
  - `Mailer.sendHtmlMail(Titulaire titulaire, String emailDestinataire, String libelle, String message)`.
  - `MailTools` choisit l'expéditeur dans la map. L'adresse `From` est le username de ce titulaire, et le nom affiché vient de `profil.getCandidatNom()`. Si le titulaire est absent de la map, `MailTools` lève `MetierException("Adresse d'envoi non configurée pour " + titulaire.getAffichage())`.
  - `AnnonceMailService.sendEmailForPendingAnnonces()` ne traite que le titulaire courant et appelle `mailer.sendHtmlMail(annonce.getTitulaire(), …)`. `sendDirectEmail(Long)` charge l'annonce par id + titulaire courant.
  - `FileAttenteController` : une `MetierException` remontée par `sendDirectEmail` devient un message flash (`RedirectAttributes`) affiché sur `/file`, au lieu d'une page d'erreur.

- [ ] **Step 1 : tests qui échouent.**
  - `MailConfigurationTest.expediteursMail_ignoreLeTitulaireSansUsername` : renee vide → la map ne contient que LAURENT.
  - `MailToolsTest.sendHtmlMail_utiliseLExpediteurEtLeNomDuTitulaire` : `From` = adresse de RENEE, nom personnel « Renée ».
  - `MailToolsTest.sendHtmlMail_titulaireNonConfigure_leveMetierException` : aucun `send` n'est appelé.
  - `AnnonceMailServiceTest.sendEmailForPendingAnnonces_nEnvoieQueLesAnnoncesDuTitulaireCourant` : vérifie `findAllByTitulaireAndStatusAnnonce(RENEE, 1)` et que `sendHtmlMail` reçoit `RENEE`.
  - `sendDirectEmail_titulaireNonConfigure_statutInchange` : le mailer lève `MetierException` et `updateStatusAnnonceEtDateEnvoi` n'est jamais appelé.
  - `FileAttenteControllerTest.postMailDirect_metierException_afficheLeMessage`.
- [ ] **Step 2 :** lancer ces classes → échec.
- [ ] **Step 3 : implémenter.** Mettre à jour les deux YAML (commentaire : Renée est facultative) et `dist.env` (`MAIL_LAURENT_*`, `MAIL_RENEE_*` vides commentés).
- [ ] **Step 4 :** `./mvnw test` → tout passe.
- [ ] **Step 5 : Commit** `feat(mail): envoie depuis l'adresse Gmail du titulaire`

---

### Task 8 : Paramètres (profil), libellés des lettres, affichage en lecture seule

**Files :**
- Modify : `controller/ParametresPageController.java`, `controller/EntreprisesPageController.java` (attribut `profil` sur la fiche), `templates/parametres.html`, `templates/entreprise_detail.html`, `templates/entreprises.html`, `templates/annonces.html`, `templates/file.html`, `templates/fragments/menu.html`, `templates/fragments/header.html` (bandeau)
- Test : `controller/ParametresPageControllerTest.java`, `controller/EntreprisesPageControllerTest.java`

**Interfaces :**
- Consumes : `ProfilService.getProfilCourant()`, `ProfilService.mettreAJour(ProfilForm)`, `ProfilService.libelleContenu(...)`, `UtilisateurCourant.estLecteur()`.
- Produces :
  - `GET /parametres` ajoute `profilForm` au modèle ;
  - `POST /parametres/profil` (`@Valid ProfilForm`) : en cas d'erreur, il réaffiche la page, sinon il redirige vers `/parametres` avec un message flash ;
  - la fiche entreprise reçoit l'attribut `profil`.

- [ ] **Step 1 : tests qui échouent.**
  - `ParametresPageControllerTest.parametresView_ajouteLeProfilCourant`.
  - `enregistrerProfil_valide_appelleMettreAJourEtRedirige`.
  - `enregistrerProfil_invalide_reafficheSansEnregistrer`.
  - `EntreprisesPageControllerTest.detail_ajouteLeProfilAuModele`.
- [ ] **Step 2 :** lancer ces classes → échec.
- [ ] **Step 3 : implémenter les contrôleurs et les vues.**
  - Parametres : section « Profil » (nom du candidat, puis libellé + poste par défaut pour chacun des 3 types).
  - `entreprise_detail.html` : les options de `contenuSelect` gardent leurs valeurs (2, 0, 1) mais affichent les libellés du profil.
  - Tous les formulaires `method="post"` sauf `/logout` et `/login`, le lien « Ajouter une entreprise » et les formulaires d'ajout de la fiche sont entourés de `sec:authorize="hasRole('TITULAIRE')"`.
  - Pour un lecteur, les éditeurs de modèles de Paramètres sont rendus en lecture : `readonly`, sans bouton.
  - Bandeau dans `header.html` : `<div sec:authorize="hasRole('LECTEUR')" class="alert alert-info">Consultation — lecture seule (France Travail)</div>`.
  - Le menu affiche le nom du titulaire consulté (`Titulaire.getAffichage()`).
- [ ] **Step 4 :** `./mvnw test` passe. Ensuite `./mvnw spring-boot:run` et vérification à la main :
  - les quatre connexions ;
  - ft01 voit les données de laurent sans aucun bouton d'action, et le bandeau est présent ;
  - renee voit un espace vide, avec « Lettre 1/2/3 » dans le formulaire d'annonce ;
  - modifier le profil de renee ne change rien chez laurent.
- [ ] **Step 5 : Commit** `feat(ui): profil dans Paramètres et affichage lecture seule pour France Travail`

---

### Task 9 : documentation et vérification finale

**Files :**
- Modify : `CLAUDE.md` (section « What this is », configuration `.env`, nouvelle section multi-utilisateurs : titulaire, `UtilisateurCourant`, règle « tout repository filtre par titulaire »), `README.md` (comptes, variables `.env`, déploiement : ajouter les nouvelles variables au `.env` **avant** `./maj.sh`)

- [ ] **Step 1 :** mettre à jour la documentation.
- [ ] **Step 2 :** `./dev-start.sh && ./mvnw clean verify` → BUILD SUCCESS. Checkstyle, PMD et SpotBugs : 0 violation. Rapport JaCoCo ≥ 80 % (indicatif).
- [ ] **Step 3 :** `grep -rn "findById(\|findAll(\|count()\|deleteById(\|CANDIDAT_NOM\|POSTE_DEFAUT\|GESTOJOB_ADMIN\|MAIL_USERNAME" src/main` → aucun appel non filtré sur les repositories métier et aucune ancienne constante.
- [ ] **Step 4 : Commit** `docs: documente le fonctionnement multi-utilisateurs`, puis ouvrir la PR `feat/multi-utilisateurs` → `master`, après accord de Laurent.
