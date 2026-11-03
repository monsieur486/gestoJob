# Obtenir un mot de passe d'application (Gmail, Yahoo)

GestoJob envoie les candidatures par SMTP depuis la boîte de chaque titulaire.
Ni Gmail ni Yahoo n'acceptent le mot de passe habituel du compte pour cet usage :
il faut un **mot de passe d'application**, c'est-à-dire un code de 16 caractères
généré par le fournisseur et réservé à une seule application. C'est ce code, et
non une « clé API », qui va dans le `.env`.

| Titulaire | Fournisseur | Variables `.env`                                | Serveur SMTP (dans `application.yml`) |
|-----------|-------------|-------------------------------------------------|---------------------------------------|
| Laurent   | Gmail       | `MAIL_LAURENT_USERNAME` / `MAIL_LAURENT_PASSWORD` | `smtp.gmail.com:587` (STARTTLS)       |
| Renée     | Yahoo       | `MAIL_RENEE_USERNAME` / `MAIL_RENEE_PASSWORD`     | `smtp.mail.yahoo.com:587` (STARTTLS)  |

`*_USERNAME` est toujours l'**adresse email complète**
(`prenom.nom@gmail.com`, `prenom.nom@yahoo.fr`).

## Gmail

1. **Activer la validation en deux étapes**, sans laquelle Google ne propose pas
   les mots de passe d'application : <https://myaccount.google.com/signinoptions/two-step-verification>.
2. Ouvrir <https://myaccount.google.com/apppasswords> (se reconnecter si demandé).
3. Saisir un nom parlant, par exemple `GestoJob`, puis **Créer**.
4. Google affiche 16 lettres groupées par quatre (`abcd efgh ijkl mnop`).
   **Les recopier sans les espaces** : c'est la valeur de `MAIL_LAURENT_PASSWORD`.
5. Le ranger aussitôt dans KeePassXC : Google ne le réaffichera plus jamais.

À savoir :

- **Changer le mot de passe du compte Google révoque tous les mots de passe
  d'application.** Il faut alors en générer un nouveau et mettre le `.env` à jour.
- Si la page `apppasswords` indique que l'option n'est pas disponible, c'est que la
  validation en deux étapes n'est pas active, ou qu'elle n'utilise que des clés
  d'accès (passkeys) : ajouter une deuxième méthode (téléphone, application
  d'authentification).
- Un ancien mot de passe d'application se révoque depuis la même page.

## Yahoo

1. Se connecter à Yahoo, puis ouvrir **Infos du compte → Sécurité du compte** :
   <https://login.yahoo.com/account/security>.
2. Si Yahoo l'exige, activer la **validation en deux étapes** sur cette même page.
3. Cliquer sur **Générer un mot de passe d'application** (ou **Gérer les mots de
   passe d'application**).
4. Saisir un nom, par exemple `GestoJob`, puis **Générer le mot de passe**.
5. Yahoo affiche 16 lettres. **Les recopier sans espaces** : c'est la valeur de
   `MAIL_RENEE_PASSWORD`.
6. Le ranger aussitôt dans KeePassXC : Yahoo ne le réaffichera plus.

À savoir :

- Comme chez Google, un mot de passe d'application se révoque depuis la page
  Sécurité du compte. Changer le mot de passe du compte Yahoo peut aussi le
  révoquer : en générer un nouveau si l'envoi échoue après un tel changement.
- Les adresses `@yahoo.fr` passent par le même serveur que les `@yahoo.com` :
  `smtp.mail.yahoo.com`, déjà réglé pour Renée dans `application.yml`.

## Mettre le mot de passe en service

Sur le serveur (`pro`) :

```bash
ssh -t pro 'nano ~/piles/gestojob/.env'      # renseigner *_USERNAME et *_PASSWORD
ssh pro 'cd ~/piles/gestojob && docker compose up -d'   # le conteneur relit le .env
```

Sans le `docker compose up -d`, le conteneur garde les anciennes valeurs.

Tant que `MAIL_RENEE_*` sont vides, l'application démarre normalement : les annonces
de Renée restent dans la file d'attente et l'envoi affiche « adresse d'envoi non
configurée ». `MAIL_LAURENT_*` sont en revanche obligatoires.

## Si l'envoi échoue

Les journaux de l'application disent pourquoi :

```bash
ssh pro 'docker logs --since 30m gestojob-webclient 2>&1 | grep -iE "AuthenticationFailed|535|envoi"'
```

| Message                                                   | Cause probable                                                                 |
|-----------------------------------------------------------|--------------------------------------------------------------------------------|
| `535-5.7.8 Username and Password not accepted` (Gmail)    | Mot de passe du compte au lieu du mot de passe d'application, code mal recopié, ou révoqué. |
| `535 5.7.0 ... Authentication failure` (Yahoo)            | Même chose côté Yahoo.                                                         |
| `Adresse d'envoi non configurée pour Renée`               | `MAIL_RENEE_USERNAME` vide dans le `.env`, ou conteneur pas relancé.           |

Une annonce dont l'envoi échoue garde son statut : elle reste dans la file
d'attente et se renvoie une fois le `.env` corrigé.
