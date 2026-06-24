# Modèles d'email éditables depuis Paramètres

**Date :** 2026-06-24
**Statut :** validé (design), prêt pour plan d'implémentation

## 1. Contexte et objectif

Les modèles de lettre de motivation (corps de l'email) et les libellés (objet de
l'email) sont aujourd'hui **codés en dur** :

- `ContenuService` contient trois corps de lettre — *général*, *microservices*,
  *IA agentique* — chacun en double version (HTML et texte brut), sous forme de
  *text blocks* Java.
- L'objet de l'email est construit dans `Annonce.getLibelle()` à partir de
  constantes de `ApplicationConfiguration` (nom du candidat, texte de candidature
  spontanée).

**Objectif :** permettre à l'utilisateur de **corriger ces modèles depuis la page
Paramètres**, avec de la mise en forme (gras, listes à puce), sans toucher au code
ni redéployer. Le corps part en HTML *et* en texte brut : on tient compte de cette
dualité.

## 2. Périmètre

**Inclus :**
- Édition des **3 contenus** (général, microservices, IA) en HTML riche.
- Édition des **2 libellés** (référence, spontanée) en texte brut.
- Persistance en base, modèles pré-remplis avec les valeurs actuelles.
- Régénération automatique du texte brut à partir du HTML.
- Nettoyage de sécurité du HTML enregistré.
- Réinitialisation d'un modèle à sa valeur d'origine.

**Hors périmètre (v1) :**
- Aperçu serveur du rendu substitué (l'éditeur montre déjà la mise en forme).
- Ajout/suppression de types de contenu (le jeu reste : général / microservices / IA).
- Historique des versions des modèles.
- Internationalisation des modèles.

## 3. Modèle de données

Nouvelle table **`modele_email`**, créée par un changelog Liquibase
`007-creation_modele_email.sql` (enregistré dans `master.xml`), pré-remplie par
`INSERT` avec les valeurs actuelles.

| Colonne          | Type           | Rôle                                                        |
|------------------|----------------|-------------------------------------------------------------|
| `id`             | identité       | clé technique                                               |
| `cle`            | varchar unique | identifiant stable du modèle (voir clés ci-dessous)         |
| `categorie`      | varchar        | `CONTENU` ou `LIBELLE`                                       |
| `format_contenu` | varchar        | `HTML` (contenus) ou `TEXTE` (libellés)                     |
| `libelle_ui`     | varchar        | nom affiché dans Paramètres (ex. « Lettre — microservices »)|
| `contenu`        | text           | le modèle lui-même (HTML pour les contenus, texte sinon)    |

**Clés (`cle`) :**
- `CONTENU_GENERAL`, `CONTENU_MICROSERVICES`, `CONTENU_IA`
- `LIBELLE_REFERENCE`, `LIBELLE_SPONTANEE`

Le mapping clé ↔ type métier réutilise les enums existants : les contenus
correspondent à `TypeContenu` (GENERAL=0, MICROSERVICES=1, IA=2) ; les libellés
correspondent à la distinction de `TypeAnnonce` (REFERENCE=1 → `LIBELLE_REFERENCE`,
sinon → `LIBELLE_SPONTANEE`).

Les **valeurs d'origine** sont aussi conservées comme **ressources classpath**
(`src/main/resources/modeles/defaut/<cle>.html|.txt`) pour alimenter à la fois le
seed Liquibase et la fonction « Réinitialiser ».

## 4. Variables (placeholders)

Convention unifiée `{{...}}`, substitution **côté serveur uniquement**.

- **Contenus** : `{{POLITESSE}}`, `{{POSTE}}` (inchangé par rapport à l'existant).
- **Libellés** : `{{REFERENCE}}`, `{{POSTE}}`, `{{NOM}}` (le nom du candidat vient de
  `ApplicationConfiguration.CANDIDAT_NOM`).

L'interface affiche, sous chaque champ, la liste des variables disponibles. Les
variables sont conservées telles quelles à l'enregistrement (le nettoyage HTML ne
doit pas les altérer).

## 5. Conception backend

### 5.1 Entité, repository, service
- `model/ModeleEmail` — entité JPA mappée sur `modele_email` (schéma owné par
  Liquibase, `ddl-auto: validate`).
- `persistance/ModeleEmailRepository` — Spring Data, `findByCle(String)`,
  `findAllByOrderByLibelleUiAsc()` (ou tri stable pour l'affichage).
- `service/ModeleEmailService` — méthodes publiques métier :
  - `getContenu(String cle)` : retourne le modèle courant (lève
    `NoSuchElementException` si la clé est inconnue).
  - `listerModeles()` : pour alimenter la page Paramètres.
  - `mettreAJour(String cle, String contenuBrut)` : nettoie (si HTML) puis enregistre.
  - `reinitialiser(String cle)` : restaure depuis la ressource classpath par défaut.

### 5.2 Génération du contenu (`ContenuService`)
`ContenuService` ne porte plus les *text blocks* : il lit le HTML depuis
`ModeleEmailService` selon `TypeContenu`.

- **HTML** : récupère le modèle HTML → échappe les saisies utilisateur
  (`{{POSTE}}`, `{{POLITESSE}}`) comme aujourd'hui → substitue → renvoie.
- **Texte** : `HtmlToPlainText.toPlainTextKeepLines(modeleHtml)` → substitue les
  variables (en clair, non échappées) → renvoie. Conséquence assumée du choix
  « source unique » : les puces deviennent « • … », le gras est perdu en texte, et
  un lien `<a href>` ne conserve que son texte (l'URL doit donc figurer dans le
  texte du lien si on veut la voir en version brute).

### 5.3 Refactor du libellé (SOLID)
`Annonce.getLibelle()` lit actuellement des constantes depuis l'entité. Comme le
libellé devient un modèle en base, sa construction sort de l'entité (une entité ne
doit pas dépendre d'un service/de la base) vers un **service dédié `LibelleService`**
(dépendant de `ModeleEmailService`). Un service dédié — plutôt qu'une méthode
d'`AnnonceService` — évite que `AnnonceMailService` et `AnnonceListeMapper` aient à
dépendre d'`AnnonceService` juste pour le libellé.

`LibelleService.construitLibelle(Annonce)` choisit le libellé `REFERENCE` ou
`SPONTANEE` selon `typeAnnonce`, puis substitue `{{REFERENCE}}`, `{{POSTE}}`,
`{{NOM}}`.

Les **trois appelants** actuels de `getLibelle()` sont adaptés :
- `AnnonceService.getAnnonceTxtContenuById`
- `AnnonceMailService.sendMail` (objet de l'email)
- `AnnonceListeMapper.fromEntity` (libellé affiché en liste)

`Annonce.getLibelle()` est retiré (ou conservé en délégation le temps de la
migration des appels, puis supprimé).

### 5.4 Sécurité — nettoyage HTML
À chaque enregistrement d'un contenu HTML, nettoyage via **jsoup `Safelist`**
restreinte. Balises autorisées : `p, br, strong, b, em, i, ul, ol, li, a[href]`.
Tout le reste est retiré. Objectifs : empêcher l'injection de HTML/JS dans les
emails et normaliser la sortie de l'éditeur. L'échappement des saisies
`{{POSTE}}`/`{{POLITESSE}}` au moment de la substitution est conservé tel quel.

## 6. Conception front-end (`parametres.html`)

- **3 éditeurs Quill** (chargés en CDN, comme Chart.js déjà présent), barre d'outils
  limitée à **gras** et **liste à puce**. Le HTML de chaque éditeur est recopié dans
  un champ caché à la soumission.
- **2 champs texte** (`input`/`textarea`) pour les libellés.
- Sous chaque champ : rappel des **variables disponibles**.
- Chaque modèle a son propre **bouton Enregistrer** (sauvegarde indépendante,
  patron PRG : POST → redirect → message flash de succès) et un bouton
  **Réinitialiser**.
- Réutilise les fragments existants (`header-css`, `menu`, `footer`) et le style
  Bootstrap en place ; messages d'erreur via le bloc `alert` déjà présent.

## 7. Flux de données

**Affichage Paramètres :** `GET /parametres` → `ModeleEmailService.listerModeles()`
→ modèles passés à la vue → Quill initialisé avec le HTML existant.

**Enregistrement d'un contenu :** `POST /parametres/modeles/{cle}` (HTML caché) →
nettoyage jsoup → `mettreAJour` → redirect `/parametres` + flash.

**Réinitialisation :** `POST /parametres/modeles/{cle}/reinitialiser` →
`reinitialiser(cle)` (ressource classpath) → redirect + flash.

**Envoi d'email (inchangé côté flux) :** `AnnonceMailService` demande à
`ContenuService` le HTML (lu en base, substitué) et à `construitLibelle` l'objet ;
l'envoi texte passe par la dérivation HTML→texte.

## 8. Gestion des erreurs

- Clé inconnue → `NoSuchElementException` (cohérent avec le reste du code).
- Échec d'enregistrement → message d'erreur en flash, la page se réaffiche.
- Lecture d'un modèle absent en base au moment de l'envoi : ne doit pas arriver
  (table seedée + `validate`) ; si la ressource par défaut est introuvable à la
  réinitialisation, erreur explicite journalisée (`error`).

## 9. Conventions et journalisation

- Tout en **français** (noms de domaine, libellés UI, Javadoc, commits).
- **Javadoc FR** avec bloc `<p><b>Exemple :</b> …</p>` avant les balises `@` sur les
  méthodes publiques ; commentaire `//` sur les méthodes privées.
- **Logs SLF4J** : `info` sur les flux métier publics (« modèle {} mis à jour »,
  « modèle {} réinitialisé »), `warn`/`error` sur les anomalies ; messages
  paramétrés, **sans donnée sensible**.
- Constantes métier éventuelles dans `ApplicationConfiguration`.

## 10. Tests (couverture ≥ 90 %)

- `ModeleEmailService` : lecture, mise à jour (avec nettoyage), réinitialisation,
  clé inconnue.
- Nettoyage jsoup : balises interdites retirées, balises autorisées conservées,
  variables `{{...}}` préservées.
- `ContenuService` : lecture du HTML depuis la base + substitution (HTML échappé) ;
  dérivation texte (puces → « • », variables substituées).
- Construction du libellé : variantes référence / spontanée + substitution.
- `ParametresPageController` : GET alimente la vue ; POST enregistre et redirige ;
  réinitialisation ; cas d'erreur (flash).
- Mise à jour des tests existants impactés par le retrait de `Annonce.getLibelle()`.

## 11. Décisions arrêtées

- **Source unique HTML**, texte brut auto-dérivé (pas de double saisie).
- **Stockage en base** via Liquibase, modèles seedés depuis des ressources classpath.
- **Quill en CDN**, barre limitée gras + puce.
- **2 libellés** distincts (référence / spontanée) avec variables.
- **Bouton Réinitialiser** inclus ; **sauvegarde par modèle** ; **pas d'aperçu
  serveur** en v1.
- Variables au format `{{...}}`, substitution serveur.

## 12. Risques / points d'attention

- Le retrait de `Annonce.getLibelle()` touche 3 appelants et leurs tests :
  refactor à mener proprement (pas de régression de libellé affiché/envoyé).
- La dérivation HTML→texte change le rendu des liens (URL perdue hors texte du
  lien) : à documenter pour l'utilisateur dans l'UI ou les modèles par défaut.
- Le seed Liquibase contient de longs blocs HTML : fichier SQL volumineux mais
  acceptable ; veiller à l'échappement des apostrophes dans les `INSERT`.
