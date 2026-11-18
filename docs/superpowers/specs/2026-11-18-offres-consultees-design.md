# Journal des offres consultées — design

## Intention

Montrer la démarche de recherche (notamment à France Travail, via les comptes lecteurs
`ft01` / `ft02`) au-delà des seules candidatures envoyées : garder une trace datée des
offres **lues puis écartées**, avec le motif de l'écart.

## Objectifs / non-objectifs

- Objectif : une saisie **rapide** (on peut lire 10 offres d'affilée), un historique daté
  et motivé, visible en lecture seule par le lecteur France Travail du titulaire.
- Objectif : le skill `gestion-annonce` peut enregistrer une offre écartée par l'API.
- Non-objectifs : pas de transformation d'une offre écartée en annonce, pas de
  statistiques sur l'accueil, pas d'export,
  pas de modification d'une entrée (suppression seulement, pour corriger une erreur).
- Anti-objectif : polluer la liste des entreprises avec des sociétés jamais contactées.

## Approches envisagées

1. **Statut « Écartée » (7) sur `Annonce`** — réutilise liste, recherche et vue lecteur,
   mais impose une entreprise (`entrepriseId` non nul) par offre lue : saisie lourde,
   entreprises fantômes.
2. **Journal séparé « Offres consultées »** — table et page dédiées, entreprise en texte
   libre. Plus léger à remplir, n'interfère pas avec les annonces. **Retenue par Laurent.**

## Design

- **Motif** : énumération `model/MotifEcart` (code + libellé, sur le modèle de
  `StatutAnnonce`) : 1 = Profil (profil non adapté), 2 = Poste (mission/poste ne convient
  pas), 3 = Distance (trop loin). Liste déroulante dans le formulaire. Pas de « Autre » :
  le commentaire libre sert à préciser.
- **Migration** `010-offres-consultees.sql` (enregistrée dans `master.xml`), table
  `offre_consultee` : `id` (identity), `titulaire` (varchar 16, non nul),
  `date_consultation` (date, non nulle), `intitule` (non nul), `entreprise` (texte libre,
  non nul), `lien` (facultatif), `motif_ecart` (int, non nul), `commentaire` (facultatif) ;
  index sur `(titulaire, date_consultation)`.
- **Couches** : entité `OffreConsultee`, `OffreConsulteeRepository` (requêtes toutes
  filtrées par titulaire, jamais `findById/findAll/deleteById` hérités),
  `OffreConsulteeService` (lister paginé, ajouter, supprimer), `OffreConsulteeForm`
  (validation), `OffreConsulteeController`.
- **Page** `/consultees` (menu « 👀 Offres consultées », visible à tout compte connecté) :
  formulaire de saisie en haut (masqué aux lecteurs via `sec:authorize`), date pré-remplie
  à aujourd'hui et modifiable ; liste paginée triée par date décroissante (date, intitulé,
  entreprise, lien cliquable, motif, commentaire, bouton supprimer pour le titulaire).
  PRG : `POST /consultees` et `POST /consultees/{id}/suppression` puis `redirect:`.
- **Sécurité** : aucune règle nouvelle ; les GET sont ouverts aux comptes connectés et
  les POST réservés à `TITULAIRE` par la règle existante `anyRequest()`. Un id de l'autre
  titulaire se comporte comme un id inconnu (404).
- **API Claude** : `POST /api/offres-consultees` (201) dans `CandidatureApiController`
  (ou un contrôleur voisin sous `controller/api/`), DTO dans `dto/api/` (intitulé,
  entreprise, lien, motif par code ou nom `PROFIL|POSTE|DISTANCE`, commentaire, date
  facultative = aujourd'hui), titulaire LAURENT comme le reste de l'API. Mêmes erreurs
  JSON (`ApiErreurConseil`). Ni lecture ni suppression par l'API.
- **Skill `gestion-annonce`** (dépôt `claude-skills`, chantier séparé, après la mise en
  prod de l'endpoint) : quand l'analyse conclut que l'offre ne correspond pas, le skill
  propose l'écart avec un motif et un commentaire, Laurent confirme, puis il enregistre
  par l'API. Il n'enregistre jamais un écart sans confirmation.
- **Pagination** : réutilise `max-annonces-par-page` (pas de nouvelle variable `.env`,
  donc rien à changer sur le serveur).

## Laissé au builder

Mise en forme exacte de la page (dans le style de `annonces.html`), messages de
validation, nom précis des méthodes. Tests unitaires pour chaque méthode publique
(seuil indicatif 80 %).
