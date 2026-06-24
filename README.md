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
  puis renseigner les vraies valeurs (identifiants base, identifiants SMTP Gmail,
  compte administrateur, tailles de pagination, `DOCKER_UI_PORT`).

## Démarrage (développement)

```bash
cp dist.env .env        # crée la config locale, puis éditer .env
./dev-start.sh          # démarre uniquement la base PostgreSQL (Docker, port 5432)
./mvnw spring-boot:run  # lance l'application sur http://localhost:8082
./dev-stop.sh           # arrête la base
```

Connexion par défaut (modifiable dans `.env`) : **utilisateur** / **Mdp12345\***.

## Build & tests

```bash
./mvnw clean verify        # build + tests + Checkstyle (bloquant) + couverture JaCoCo (≥ 90 %)
./mvnw clean verify site   # idem + site Maven (infos, Javadoc FR, JaCoCo, Surefire) dans target/site/
```

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

## Configuration

Toute la configuration provient de variables d'environnement (aucune valeur en dur) :
`application.yml` importe `optional:file:.env`. Le modèle versionné est **`dist.env`** ;
le `.env` réel est **gitignoré**. Toute évolution du schéma passe par un changelog
**Liquibase** (`src/main/resources/db/changelog/`), jamais par Hibernate.
