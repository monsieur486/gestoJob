# Conception — GestoJob multi-utilisateurs (laurent, renée, ft01, ft02)

Date : 2026-11-02

## Intention

GestoJob a été conçu pour un seul candidat (Laurent). Une seconde personne,
**Renée**, veut s'en servir pour sa propre recherche d'emploi, sur un autre
profil professionnel, avec sa propre adresse Gmail. Chacun doit retrouver un
outil identique à celui d'aujourd'hui, mais sans jamais voir les données de
l'autre.

Par ailleurs, lors d'un entretien (téléphonique ou sur place) avec un
conseiller **France Travail**, le candidat doit pouvoir **montrer ses démarches
de recherche d'emploi** sans risque qu'une manipulation les modifie. D'où deux
comptes en lecture seule : **ft01** (miroir de laurent) et **ft02** (miroir de
renée).

## Objectifs

1. Quatre comptes de connexion :

   | Compte | Rôle      | Voit les données de | Peut modifier / envoyer |
   |--------|-----------|---------------------|--------------------------|
   | laurent | titulaire | laurent            | oui                      |
   | renée  | titulaire | renée               | oui                      |
   | ft01   | lecteur   | laurent             | non                      |
   | ft02   | lecteur   | renée               | non                      |

2. **Cloisonnement total** des données entre laurent et renée : entreprises,
   contacts, annonces, modèles d'email, profil. Aucune donnée partagée.
3. Toutes les données existantes appartiennent à **laurent** après migration.
   Renée démarre avec un espace vide (modèles d'email par défaut).
4. Chaque titulaire envoie ses candidatures **depuis sa propre adresse Gmail**,
   avec son propre nom d'expéditeur.
5. Chaque titulaire dispose de **trois types de lettre** (les trois emplacements
   actuels : codes `TypeContenu` 1, 2 et « autre »), dont il **nomme** chacun et
   fixe l'**intitulé de poste par défaut** dans Paramètres.
   - laurent : valeurs actuelles (« général », « microservices »,
     « IA agentique » et les `POSTE_DEFAUT_*` actuels).
   - renée : libellés neutres par défaut (ex. « Lettre 1/2/3 »), à renommer.
6. Chaque titulaire règle son **nom de candidat** (expéditeur + libellés) dans
   Paramètres.
7. Un lecteur (ft01/ft02) voit **tout ce que voit son titulaire** — listes,
   fiches entreprise, contacts (noms, emails), annonces, file d'attente, texte
   des lettres, Paramètres — mais **en lecture seule**.

## Non-objectifs

- Pas d'écran de gestion des utilisateurs, pas d'inscription, pas de
  changement de mot de passe en ligne : les quatre comptes sont fixes.
- Pas de partage de données entre titulaires (même entreprise saisie deux
  fois = deux fiches indépendantes).
- Pas de nouveau type de lettre au-delà de trois.
- Pas d'historique d'accès des conseillers France Travail.

## Anti-objectifs (échec même si « ça marche »)

- Renée (ou ft02) peut voir, même une seule fois, une donnée de laurent — y
  compris en tapant un identifiant dans l'URL (`/entreprises/{id}`,
  `/contenu/{id}`…) ou via la recherche, les compteurs, la pagination.
- Un mail de renée part de l'adresse de laurent (ou l'inverse), ou avec le
  mauvais nom d'expéditeur.
- Un lecteur parvient à modifier quoi que ce soit, ou à déclencher un envoi,
  même en forgeant une requête POST. La lecture seule ne doit **pas** reposer
  uniquement sur le masquage des boutons.
- La migration perd ou désattribue des données existantes de laurent.

## Contraintes

- Stack et conventions existantes (Spring Boot 4, Thymeleaf, Liquibase,
  français partout, Javadoc française, tests ≥ 80 % indicatif, Checkstyle/PMD/
  SpotBugs sans violation).
- **Schéma via Liquibase uniquement** : nouveau changelog `008-…sql` enregistré
  dans `master.xml` ; `ddl-auto: validate` reste.
- **Secrets dans `.env`** (mots de passe des comptes, identifiants Gmail) ;
  `dist.env` mis à jour. Constantes métier non secrètes dans
  `ApplicationConfiguration`.
- Chantier sur **branche dédiée + PR**, commits en français, sans commit
  automatique (relecture fichier par fichier par Laurent).
- Null-safety systématique.

## Décisions prises

### Identité et rôles

- Deux **titulaires** fixes, identifiés par un code stable indépendant du login
  (décision de conception : un enum `Titulaire { LAURENT, RENEE }`). Le code
  stocké en base est ce code, pas le login, pour qu'un changement de login dans
  `.env` n'orpheline pas les données.
- Quatre comptes en mémoire (`InMemoryUserDetailsManager`, comme aujourd'hui),
  logins et mots de passe lus depuis `.env`. Le compte `GESTOJOB_ADMIN_*`
  disparaît, remplacé par :

  ```
  GESTOJOB_LAURENT_USERNAME=laurent      GESTOJOB_LAURENT_PASSWORD=…
  GESTOJOB_RENEE_USERNAME=renee          GESTOJOB_RENEE_PASSWORD=…
  GESTOJOB_FT01_USERNAME=ft01            GESTOJOB_FT01_PASSWORD=…
  GESTOJOB_FT02_USERNAME=ft02            GESTOJOB_FT02_PASSWORD=…
  ```

  Le rattachement ft01→LAURENT, ft02→RENEE est structurel (dans le code), pas
  configurable. Login proposé `renee` sans accent (saisie plus sûre) ;
  l'affichage peut dire « Renée ».
- Deux rôles Spring Security : `TITULAIRE` et `LECTEUR`.
- Un composant unique (ex. `UtilisateurCourant`) résout, à partir de
  l'authentification, le **titulaire dont on affiche les données**. Tous les
  services passent par lui ; aucun contrôleur ne choisit le titulaire à partir
  d'un paramètre de requête.

### Données

- Colonne `titulaire VARCHAR(16) NOT NULL` sur `entreprises`, `contacts`,
  `annonces`, `modele_email` (dénormalisée sur les quatre tables : les entités
  n'ont pas de relations JPA, et les requêtes d'annonces sont nombreuses).
  Migration : ajout avec défaut `'LAURENT'`, puis `NOT NULL`, puis suppression
  du défaut ; index sur `titulaire`.
- Toute lecture ou écriture est filtrée par titulaire : listes, pages,
  recherche, compteurs, `existsBy…` (ex. unicité du nom d'entreprise et de
  l'email de contact **par titulaire**), `findById` (un id d'un autre titulaire
  se comporte comme inexistant → même réponse qu'un id inconnu).
- `modele_email` : la contrainte `uq_modele_email_cle` devient unique sur
  `(titulaire, cle)`. Le cache de `ModeleEmailService` est indexé par
  titulaire + clé. L'initialisation au démarrage crée les modèles par défaut
  manquants **pour chaque titulaire** (renée obtient donc les modèles par
  défaut ; laurent garde les siens).
- **Modèles par défaut par titulaire** : les fichiers actuels de
  `src/main/resources/modeles/defaut/` sont les lettres de laurent et restent
  ses défauts (initialisation et « Réinitialiser »). Un second jeu, **neutre**,
  est créé pour renée : trame courte utilisant `{{POLITESSE}}` et `{{POSTE}}`,
  objets sans mention « développeur Java ». Le chemin de la ressource par
  défaut dépend donc du titulaire (`CleModele.getCheminRessource()` ou
  équivalent). Aucun texte de laurent ne doit apparaître chez renée/ft02.
- Nouvelle table `profil` (une ligne par titulaire) : nom du candidat, et pour
  chacun des trois types de lettre son libellé et son intitulé de poste par
  défaut. Migration : ligne LAURENT avec les valeurs actuelles
  (`CANDIDAT_NOM`, `POSTE_DEFAUT_*`, libellés actuels) ; ligne RENEE avec des
  valeurs neutres. `CANDIDAT_NOM`, `DEFAULT_EMAIL_FROM` et `POSTE_DEFAUT_*`
  quittent `ApplicationConfiguration` (les civilités et la salutation
  générique y restent : communes aux deux).
- Les libellés « général / microservices / IA agentique » codés en dur
  (`CleModele.getLibelleUi()`, options du formulaire d'annonce, `TypeContenu`
  libellés) sont remplacés à l'affichage par ceux du profil courant. Les codes
  1/2/autre et les clés `CONTENU_*` restent inchangés.

### Page d'accueil

- `/` n'est **plus publique** : elle exige une connexion et n'affiche que les
  chiffres et les annonces positives du titulaire courant (lecteurs compris).
  Un visiteur non connecté est redirigé vers `/login`. Restent publics :
  `/login`, `/css/**`, `/js/**`, `/favicon.ico`. Le menu des visiteurs non
  connectés est ajusté en conséquence.

### Envoi de mail

- `.env` : `MAIL_LAURENT_USERNAME/PASSWORD`, `MAIL_RENEE_USERNAME/PASSWORD`
  (remplacent `MAIL_USERNAME/PASSWORD`). Même hôte SMTP Gmail.
- `MAIL_LAURENT_*` restent obligatoires (démarrage refusé sinon, comme
  aujourd'hui). `MAIL_RENEE_*` sont **facultatifs** (décision de conception,
  pour pouvoir déployer avant que son compte Gmail soit prêt) : tant qu'ils
  manquent, l'application démarre, les annonces de renée restent dans la
  file d'attente et toute tentative d'envoi affiche un message d'erreur clair
  (« adresse d'envoi non configurée »), sans changer de statut.
- L'envoi choisit l'expéditeur (adresse + session SMTP) et le nom affiché selon
  le titulaire de l'annonce envoyée, jamais selon une valeur globale.

### Lecture seule

- **Côté serveur** : `SecurityConfig` refuse toute requête non-GET aux comptes
  `LECTEUR` (hors `/logout`), ainsi que les pages GET de formulaire de
  création (`/entreprises/ajout`, `/entreprises/{id}/contacts/ajout`, et
  toute autre page dont la seule raison d'être est une saisie). Refus = 403.
- **Côté affichage** : boutons et formulaires d'action masqués pour les
  lecteurs (`sec:authorize`, `thymeleaf-extras-springsecurity6` déjà présent).
  Les éditeurs de modèles dans Paramètres s'affichent en lecture.
- Un bandeau discret indique « Consultation — lecture seule (France Travail) »
  quand un lecteur est connecté.

## Laissé au constructeur

- Nom exact des classes, du composant de résolution du titulaire, découpage
  des services ; manière de construire les deux `JavaMailSender`.
- Forme de l'écran Profil dans Paramètres (même page, section dédiée).
- Libellés neutres par défaut pour renée.
- Écriture des tests (sécurité : un test par règle de lecture seule et de
  cloisonnement au minimum).
- Mise à jour de `CLAUDE.md` et du README pour refléter le multi-utilisateur.
