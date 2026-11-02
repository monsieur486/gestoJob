# GestoJob

Suivi personnel de candidatures : application web rendue côté serveur permettant de
gérer des **entreprises**, leurs **contacts** et les **annonces** (candidatures), puis
d'envoyer les courriels de lettre de motivation depuis une file d'attente.

Stack : **Java 17**, **Spring Boot 4**, **Maven**, **PostgreSQL** (schéma géré par
**Liquibase**), **Thymeleaf**, **Lombok**, **Jakarta Mail** (SMTP Gmail).

## Prérequis

- **Java 17** (JDK)
- **Maven** via le wrapper `./mvnw` (rien à installer)
- **Docker** + **Docker Compose** (pour la base PostgreSQL)
- Un fichier **`.env`** à la racine, copié depuis le modèle : `cp dist.env .env`
  puis renseigner les vraies valeurs (identifiants base, identifiants SMTP Gmail de
  chaque titulaire, comptes de connexion, tailles de pagination, `DOCKER_UI_PORT`).

## Démarrage (développement)

```bash
cp dist.env .env        # crée la config locale, puis éditer .env
./dev-start.sh          # démarre uniquement la base PostgreSQL (Docker, port 5432)
./mvnw spring-boot:run  # lance l'application sur http://localhost:8082
./dev-stop.sh           # arrête la base
```

Les comptes de connexion et leurs mots de passe sont définis dans `.env`
(`GESTOJOB_*_USERNAME` / `GESTOJOB_*_PASSWORD`, obligatoires, sans valeur par défaut) ;
`dist.env` propose des valeurs d'exemple à changer.

## Comptes et multi-utilisateurs

L'application sert deux candidats, dont les données sont **totalement cloisonnées**
(entreprises, contacts, annonces, modèles d'email, profil) :

| Compte | Rôle | Données vues | Modification / envoi |
|--------|------|--------------|----------------------|
| `laurent` | titulaire | celles de Laurent | oui |
| `renee` | titulaire | celles de Renée | oui |
| `ft01` | lecteur (France Travail) | celles de Laurent | non, lecture seule |
| `ft02` | lecteur (France Travail) | celles de Renée | non, lecture seule |

- La page d'accueil exige une connexion ; seuls `/login` et les ressources statiques sont publics.
- Les comptes lecteurs servent à montrer les démarches à un conseiller France Travail : toute requête
  de modification est refusée côté serveur (403), pas seulement masquée.
- Chaque titulaire règle dans **Paramètres** son nom de candidat, le libellé et l'intitulé de poste par
  défaut de ses trois types de lettre, ainsi que ses modèles d'email.
- Chaque titulaire envoie ses courriels depuis **sa propre adresse** : Gmail pour Laurent
  (`MAIL_LAURENT_*`, obligatoire), Yahoo pour Renée (`MAIL_RENEE_*`, facultatif : sans eux, les
  annonces de Renée restent dans la file d'attente et l'envoi affiche une erreur explicite). Les
  deux comptes exigent un **mot de passe d'application** ; l'hôte SMTP de chaque titulaire est
  réglé dans `application.yml` (`gestojob.mail.<titulaire>.host|port`).

## Build & tests

```bash
./mvnw clean verify        # build + tests + analyses qualité (Checkstyle, PMD, SpotBugs) + couverture JaCoCo
./mvnw clean verify site   # idem + site Maven (infos, Javadoc FR, JaCoCo, Surefire) dans target/site/
```

> Les analyses de qualité (Checkstyle, PMD, SpotBugs + FindSecBugs) et le contrôle
> de couverture JaCoCo (objectif ≥ 80 % des lignes) sont **indicatifs, non
> bloquants** : ils signalent les écarts sans casser le build. L'objectif reste
> néanmoins 0 violation.

> `./mvnw verify` exécute la suite complète, dont un test qui démarre le contexte
> Spring : il faut donc que la base soit lancée (`./dev-start.sh`).

## Déploiement (prod-like)

```bash
./prod-start.sh   # construit l'image et démarre la pile complète (base + application) via Docker Compose
./prod-stop.sh    # arrête la pile complète
./maj.sh          # mise à jour : arrêt, git pull, reconstruction et redémarrage de la pile
```

En conteneur, le profil `docker` est actif (`SPRING_PROFILES_ACTIVE=docker`) et la
base est jointe en interne (`gestojob-db`). L'application est exposée sur le port
`DOCKER_UI_PORT` (par défaut `8082`), destiné à être servi derrière un reverse proxy.

**Avant `./maj.sh`** (première mise à jour vers la version multi-utilisateurs), compléter le
`.env` du serveur : remplacer `MAIL_USERNAME/PASSWORD` par `MAIL_LAURENT_USERNAME/PASSWORD`
(et éventuellement `MAIL_RENEE_*`), supprimer `GESTOJOB_ADMIN_*` et ajouter les huit variables
`GESTOJOB_{LAURENT,RENEE,FT01,FT02}_{USERNAME,PASSWORD}` (voir `dist.env`). La migration Liquibase
`008` rattache toutes les données existantes à Laurent.

## Configuration

Toute la configuration provient de variables d'environnement (aucune valeur en dur) :
`application.yml` importe `optional:file:.env`. Le modèle versionné est **`dist.env`** ;
le `.env` réel est **gitignoré**. Toute évolution du schéma passe par un changelog
**Liquibase** (`src/main/resources/db/changelog/`), jamais par Hibernate.

## Architecture

Application en couches classiques (`controller → service → persistance`), avec des
**DTO aux frontières** (jamais l'entité JPA exposée) et des contrôleurs minces. La
logique métier est portée par des services à responsabilité unique, découpés par
domaine :

**Annonces**

- `AnnonceService` — création, consultation, suppression et recherche paginée des annonces.
- `AnnonceStatutService` — transitions de statut du cycle de vie (dépassée, refusée, acceptée).
- `AnnonceTexteService` — formule de politesse et contenu **texte** de l'annonce (copier-coller).
- `AnnonceMailService` — envoi des courriels (contenu **HTML**), individuel ou depuis la file d'attente ; l'expédition est déléguée à l'abstraction **`Mailer`** (implémentation `MailTools`, SMTP), ce qui découple le service du transport et facilite les tests.
- `AnnonceListeMapper` — projection des annonces en DTO d'affichage, avec chargement groupé (anti-N+1).

**Entreprises & contacts**

- `EntrepriseService` — gestion des entreprises (création, mise à jour, activation/désactivation).
- `EntrepriseRechercheService` — listes et recherches paginées d'entreprises.
- `ContactService` — gestion des contacts rattachés aux entreprises.

**Contenu & courriels**

- `ContenuService` — modèles de lettre de motivation (variantes HTML et texte selon le type de poste).
- `ModeleEmailService` — modèles d'email éditables et leur cache.
- `LibelleService` — construction de l'objet (libellé) du courriel de candidature.
- `HtmlConverterService` — conversion HTML → texte brut.

**Cloisonnement** — `UtilisateurCourant` (package `securite`) déduit le titulaire
(`Titulaire.LAURENT` ou `RENEE`) de l'authentification ; tous les repositories métier
filtrent par titulaire, et un identifiant de l'autre titulaire se comporte comme un
identifiant inexistant. `ProfilService` gère le profil de chaque titulaire.

Les erreurs fonctionnelles destinées à l'utilisateur remontent via une exception
métier typée `MetierException` (message affichable), distincte des erreurs
techniques qui, elles, ne sont pas exposées.
