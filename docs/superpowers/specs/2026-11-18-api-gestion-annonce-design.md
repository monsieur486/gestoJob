# Conception — API GestoJob et skill `gestion-annonce`

Date : 2026-11-18

## Intention

Laurent répond à des offres d'emploi. Deux irritants aujourd'hui :

1. **surtout**, la lettre de motivation est trop générique : une annonce ne fait que
   choisir l'un des trois modèles (`typeContenu`), dont seuls `{{POLITESSE}}` et
   `{{POSTE}}` varient ;
2. la ressaisie de l'entreprise, du contact et de l'annonce dans l'interface.

Le but : Laurent colle une offre dans Claude Code, le skill `gestion-annonce`
l'analyse, **personnalise sa lettre « IA agentique »** (qu'il a déjà retravaillée,
dans Paramètres) et enregistre la candidature dans GestoJob **en production** via
une nouvelle API.

## Objectifs

1. Une API REST `/api/**` dans GestoJob, utilisable par Claude Code depuis le poste
   de Laurent vers l'instance de prod (derrière le reverse proxy, joignable depuis
   Internet).
2. Une **lettre personnalisée par annonce**, stockée avec l'annonce et utilisée à
   l'envoi à la place du modèle.
3. Un skill `gestion-annonce` qui enchaîne : analyse → lettre → relecture par
   Laurent → enregistrement.

## Non-objectifs

- Renée et les comptes France Travail : l'API ne sert **que** le titulaire `LAURENT`.
- Envoi d'email par l'API : l'API n'envoie **jamais** de mail. L'annonce part en
  file d'attente (`/file`) et Laurent clique « envoyer », comme aujourd'hui.
- Édition de la lettre personnalisée dans l'interface web (v1) : on la relit dans
  la conversation avant écriture ; une correction ultérieure se fait en supprimant
  et recréant l'annonce.
- Serveur MCP, Spring AI : écartés (voir Approches).
- Mise à jour / suppression d'annonces par l'API.

## Anti-objectifs (échec même si « ça marche »)

- Une lettre enregistrée ou envoyée sans que Laurent l'ait vue.
- Une brèche dans la sécurité existante : la chaîne des formulaires (session,
  CSRF, rôles) doit rester strictement inchangée ; aucune donnée de Renée
  accessible par l'API.
- Une lettre qui dénature celle de Laurent (réécriture au-delà du périmètre convenu).
- Des doublons d'entreprise ou de contact créés silencieusement.
- Le jeton qui apparaît dans un log, une transcription ou un dépôt git.

## Contraintes

- Conventions du projet (CLAUDE.md) : français, couches controller → service →
  persistance, DTO aux frontières, contrôleurs minces, Javadoc française, SLF4J,
  tests JUnit 5 + Mockito (contrôleurs testés directement), JaCoCo ~80 % indicatif.
- Schéma géré par **Liquibase** : nouvelle migration `009-…sql` dans `master.xml`.
- Cloisonnement : toute requête métier filtrée par titulaire (`…AndTitulaire`),
  jamais `findById/findAll` hérités. Le titulaire vient de l'authentification
  (`UtilisateurCourant`), jamais de la requête.
- Secrets : jeton dans le `.env` (modèle `dist.env`), côté poste dans le sas
  (`~/F0rtkn0x/`), manipulé selon le skill `sas-securise`.
- Pas de commit automatique : Laurent relit les diffs fichier par fichier.

## Approches envisagées

1. **API REST dans GestoJob + skill qui appelle en `curl`** — *choisie par Laurent.*
   Suit les patterns existants, aucune dépendance nouvelle, testable en JUnit.
   Coût : le skill gère le jeton et construit le JSON.
2. **Serveur MCP dans GestoJob** — outils natifs pour Claude, mais Spring AI à
   ajouter (compatibilité Boot 4.0.2 incertaine), transport HTTP/SSE à faire passer
   par le proxy ; trop lourd pour trois opérations. Reste possible plus tard
   au-dessus des mêmes services.
3. **Écriture directe en base (SSH + psql)** — écartée : contourne le cloisonnement
   et la validation des services, et ce n'est pas une API.

## Décisions de Laurent

- Utilisateur de l'API : **laurent** uniquement.
- Personnalisation **ciblée (option A)** : la lettre de base est le modèle
  `CONTENU_IA` de Laurent **tel qu'en base**. Le skill réécrit **l'accroche**
  (1er paragraphe de texte après la politesse) et **le paragraphe « Vous rejoindre »**
  pour citer l'entreprise, la mission et 2-3 exigences de l'offre ; il **peut
  réordonner** les technologies du paragraphe « socle technique » pour mettre en
  avant celles de l'offre (sans en ajouter ni en retirer). Tout le reste est
  conservé à l'identique (orchestration d'agents, projets, entretien, formule
  finale, signature, liens CV/site).
- Cible : l'instance de **production**.
- Approche : **1** (API REST + `curl`).
- Jeton refusé : **alimente fail2ban** (option a), avec le format de ligne existant.

## Conception

### 1. Authentification de l'API

- Variable `GESTOJOB_API_TOKEN` (`.env`, ajoutée à `dist.env` avec un commentaire :
  « jeton de l'API Claude, ≥ 32 octets aléatoires ; vide = API désactivée »),
  lue dans `application.yml` sous `gestojob.api.token` avec valeur par défaut vide.
- **Seconde `SecurityFilterChain`**, `@Order` prioritaire, `securityMatcher("/api/**")` :
  sans état (`STATELESS`), CSRF désactivé *pour cette chaîne seulement*, pas de
  formLogin, toute requête authentifiée.
- Un filtre lit l'en-tête `X-GestoJob-Jeton: <jeton>`, compare en **temps constant**
  (`MessageDigest.isEqual`) au jeton configuré, et pose une authentification
  portant `ROLE_TITULAIRE` + `DONNEES_LAURENT`. Ainsi `UtilisateurCourant` et
  tous les services existants fonctionnent sans modification.
- Jeton absent, faux, ou jeton configuré vide → **401** JSON, sans détail.
- La chaîne existante (`SecurityConfig.securityFilterChain`) n'est pas modifiée.
- Ne jamais logger le jeton ni l'en-tête `Authorization`.
- **fail2ban (décision de Laurent, option a)** : un jeton refusé écrit sur le logger
  `securite.connexion` **exactement** la ligne du contrat existant
  (`EcouteurEchecConnexion`) : `ECHEC_CONNEXION identifiant="api" adresse=<ip>`,
  l'adresse étant l'adresse cliente réelle (`request.getRemoteAddr()`, déjà corrigée
  par `forward-headers-strategy: native`). Le filtre fail2ban existant la prend
  donc en compte sans modification. Précision : `native` est la valeur de
  `application.yml` ; le profil `docker` (`application-docker.yml`) la remplace par
  `framework`, qui traite le schéma et l'hôte mais ne remplace pas forcément
  l'adresse distante : à vérifier en production si l'adresse journalisée est celle du proxy. Moyen au choix du builder (écriture directe
  dans le filtre, ou passage par un `AuthenticationManager` qui publie l'événement
  d'échec), mais **une seule ligne par refus**, et le format ne doit pas diverger.
  Risque accepté : des refus répétés depuis le poste de Laurent bannissent son
  adresse ; le skill s'arrête donc **au premier 401**, sans réessayer.
- **En-tête** *(choix de conception)* : le jeton est passé dans l'en-tête dédié
  `X-GestoJob-Jeton` plutôt que `Authorization`, pour ne pas entrer en conflit avec
  un éventuel middleware Traefik (basic/forward-auth) — le dépôt d'infra n'étant
  pas visible d'ici. Partout où ce document dit `Authorization: Bearer`, lire
  `X-GestoJob-Jeton`.

### 2. Points d'entrée (package `controller.api`, `@RestController`, JSON)

| Méthode | Chemin | Rôle |
|---|---|---|
| `GET`  | `/api/modele-lettre` | Lettre IA de Laurent telle qu'en base : `{ "cle": "CONTENU_IA", "html": "…", "posteParDefaut": "…" }` |
| `GET`  | `/api/entreprises?nom=…` | Recherche de doublons (insensible à la casse, sous-chaîne) : liste `{ id, nom, ville, codePostal, estActive, contacts: [{ id, nom, email, formuleDePolitesse }] }` |
| `POST` | `/api/candidatures` | Crée la candidature en **une transaction** (voir ci-dessous) |

Corps de `POST /api/candidatures` :

```json
{
  "entreprise": { "id": 12 },                       // OU, pour en créer une :
  "entreprise": { "nom": "…", "adresse1": "…", "adresse2": null,
                  "codePostal": "…", "ville": "…" },
  "contact": { "id": 34 },                          // OU création :
  "contact": { "email": "…", "nom": "…", "formuleDePolitesse": 1 },
  "contact": null,                                  // pas de contact = candidature « site »
  "annonce": { "typeAnnonce": 1, "poste": "…", "reference": "…" },
  "lettreHtml": "<p>{{POLITESSE}}</p>…"
}
```

Règles :
- `entreprise.id` et `contact.id` doivent appartenir à Laurent, sinon **404**
  (même réponse qu'un id inconnu). Un `contact.id` doit être rattaché à l'entreprise.
- Création d'entreprise / contact : mêmes validations que les formulaires
  (`EntrepriseForm`, `ContactForm`, Bean Validation). Une nouvelle entreprise est active.
- **Entreprise existante inactive** *(choix de conception)* : la candidature la
  **réactive** dans la même transaction (`EntrepriseService.setActive`) ; la réponse
  porte `entrepriseReactivee: true`. Le skill le signale à Laurent à l'étape 5,
  avant le feu vert.
- Annonce : `typeContenu = 2` (IA agentique), statut et date selon la règle existante
  (`AnnonceCreationService.saveForm`, extrait d'`AnnonceService`) : avec contact → `BOITE_ENVOI` (1) ; sans contact →
  « site », `EN_COURS` (2), `dateEnvoi = now`.
- `lettreHtml` : passée par `HtmlSanitizer.nettoie`, **doit contenir `{{POLITESSE}}`**
  (la salutation reste tranchée à l'envoi d'après le contact) ; `{{POSTE}}`
  facultatif (substitué s'il est présent). Sinon **400**.
- Réponse **201** : `{ annonceId, entrepriseId, contactId, statut, entrepriseCreee,
  entrepriseReactivee, contactCree }`.
- Erreurs en JSON `{ "erreur": "message" }` via un `@RestControllerAdvice` limité au
  package `controller.api` (`MetierException`/validation → 400,
  `NoSuchElementException` → 404), pour que `ErreurConseil` (page HTML 404) ne
  s'applique pas à l'API.
- Logique de création dans un **service dédié** (ex. `CandidatureApiService`), qui
  réutilise `EntrepriseService`, `ContactService`, `AnnonceCreationService` ; le contrôleur
  reste mince et ne manipule que des DTO (`record`s ou classes Lombok dans `dto/`).

### 3. Lettre personnalisée par annonce

- Migration `009-lettre-personnalisee.sql` : `ALTER TABLE annonces ADD COLUMN
  lettre_personnalisee TEXT NULL;` enregistrée dans `master.xml`.
- `Annonce.lettrePersonnalisee` (String, nullable).
- `ContenuService` : là où le HTML du modèle est lu (`getContent`), si l'annonce a
  une lettre personnalisée non vide, elle **remplace** le modèle ; la substitution
  `{{POLITESSE}}`/`{{POSTE}}` et la variante texte (`HtmlToPlainText`) s'appliquent
  à l'identique. Conséquence : l'email HTML (`AnnonceMailService`), le texte de
  copier-coller (`AnnonceTexteService`, `/contenu/{id}`) utilisent tous la lettre
  personnalisée. L'objet de l'email (`LibelleService`) ne change pas.
- Annonce sans lettre personnalisée : comportement actuel inchangé (toutes les
  annonces existantes, et celles créées par l'interface).

### 4. Skill `gestion-annonce`

- Emplacement : `~/.claude/skills/gestion-annonce/SKILL.md`, versionné dans le dépôt
  `claude-skills` (mise à jour via `maj-skills.sh`), à la manière des autres skills.
- Configuration côté poste : URL de base de la prod et chemin du jeton dans le sas ;
  le jeton est passé à `curl` **par fichier** (ex. `curl -H @<fichier-en-tête>`),
  jamais affiché, jamais tapé dans une commande — conformément à `sas-securise`.
- Déroulé :
  1. Laurent colle l'offre. Le skill en extrait : entreprise (nom, adresse, ville,
     code postal), poste, référence, contact (nom, civilité, email) si présent,
     type (`1` si l'offre a une référence / est une annonce, `0` si spontanée),
     mission et 3-5 exigences clés.
  2. `GET /api/modele-lettre` → lettre de base.
  3. `GET /api/entreprises?nom=…` → doublons. Un seul candidat évident : le
     proposer en réutilisation ; plusieurs : demander à Laurent ; aucun : création.
     Idem pour le contact (même email = même contact).
  4. Rédige la lettre selon l'option A (périmètre strict ci-dessus), en gardant
     `{{POLITESSE}}` aux deux emplacements.
  5. **Montre** à Laurent : l'analyse (tableau des champs), le choix entreprise /
     contact (réutilisation ou création), et la lettre — en signalant les deux
     paragraphes réécrits et l'éventuel réordonnancement. Attend un feu vert
     explicite ; applique les corrections demandées.
  6. `POST /api/candidatures` ; affiche le résultat (ids, statut) et rappelle que
     l'envoi se fait depuis `/file` (ou que c'est une candidature « site » déjà
     marquée envoyée).
  7. Sur un 401 : s'arrêter immédiatement, sans réessayer (risque de bannissement
     fail2ban), et dire à Laurent de vérifier le fichier de jeton.
- Le skill n'invente rien sur le profil de Laurent : il ne s'appuie que sur la
  lettre de base et l'offre.

### 5. Déploiement

- Ajouter `GESTOJOB_API_TOKEN` au `.env` du serveur avant `./maj.sh` (documenté dans
  le README, section Déploiement, et dans `dist.env`).
- Vérifier que Traefik transmet `/api/**` et l'en-tête `X-GestoJob-Jeton`
  (aucun changement d'infra attendu).
- Accès HTTPS uniquement (porté par le proxy).

### 6. Tests

- Filtre jeton : bon jeton → authentification `ROLE_TITULAIRE`/`DONNEES_LAURENT` ;
  jeton faux / absent / configuré vide → 401 **et** une ligne
  `ECHEC_CONNEXION identifiant="api" adresse=…` (format vérifié).
- Isolation des chaînes : un POST de formulaire sans CSRF reste refusé ; `/api/**`
  n'accepte pas la session de formulaire comme substitut au jeton (au choix du
  builder, test de configuration ou d'intégration léger).
- `CandidatureApiService` : entreprise existante / créée, contact existant / créé /
  absent, id d'un autre titulaire → 404, contact d'une autre entreprise → 404,
  lettre sans `{{POLITESSE}}` → refus, HTML nettoyé, statut 1 vs 2.
- `ContenuService` : lettre personnalisée prioritaire sur le modèle (HTML et texte),
  modèle utilisé sinon.
- Contrôleurs API testés directement (sans MockMvc), conformément au projet.

## Laissé au builder

- Noms exacts des classes, DTO et du filtre ; découpage interne du service.
- Forme précise de la recherche d'entreprise (`…ContainingIgnoreCaseAndTitulaire`
  ou `@Query`), et limite du nombre de résultats.
- Format exact des messages d'erreur.
- Rédaction détaillée du `SKILL.md` (consignes de style de la lettre, exemples).
