package com.mr486.gestojob.model;

/**
 * Catalogue des modèles d'email éditables : associe chaque clé stable à sa
 * catégorie, son format, son libellé d'affichage, sa ressource par défaut et les
 * variables substituables. Centralise le mapping depuis les types métier
 * ({@link TypeContenu}, {@link TypeAnnonce}), dans l'esprit des autres enums du
 * domaine (principe ouvert/fermé).
 */
public enum CleModele {

    CONTENU_GENERAL(Categorie.CONTENU, true, "Lettre — général",
            "modeles/defaut/CONTENU_GENERAL.html", "{{POLITESSE}}, {{POSTE}}"),
    CONTENU_MICROSERVICES(Categorie.CONTENU, true, "Lettre — microservices",
            "modeles/defaut/CONTENU_MICROSERVICES.html", "{{POLITESSE}}, {{POSTE}}"),
    CONTENU_IA(Categorie.CONTENU, true, "Lettre — IA agentique",
            "modeles/defaut/CONTENU_IA.html", "{{POLITESSE}}, {{POSTE}}"),
    LIBELLE_REFERENCE(Categorie.LIBELLE, false, "Objet — candidature à une référence",
            "modeles/defaut/LIBELLE_REFERENCE.txt", "{{REFERENCE}}, {{NOM}}, {{POSTE}}"),
    LIBELLE_SPONTANEE(Categorie.LIBELLE, false, "Objet — candidature spontanée",
            "modeles/defaut/LIBELLE_SPONTANEE.txt", "{{NOM}}, {{POSTE}}");

    /** Nature du modèle : corps de lettre ou objet d'email. */
    public enum Categorie { CONTENU, LIBELLE }

    private final Categorie categorie;
    private final boolean html;
    private final String libelleUi;
    private final String cheminRessource;
    private final String variables;

    CleModele(Categorie categorie, boolean html, String libelleUi,
              String cheminRessource, String variables) {
        this.categorie = categorie;
        this.html = html;
        this.libelleUi = libelleUi;
        this.cheminRessource = cheminRessource;
        this.variables = variables;
    }

    /**
     * Retourne la catégorie de ce modèle (corps de lettre ou objet d'email).
     *
     * <p><b>Exemple :</b> {@code CONTENU_GENERAL.getCategorie()} retourne {@code Categorie.CONTENU}.</p>
     *
     * @return la catégorie du modèle
     */
    public Categorie getCategorie() {
        return categorie;
    }

    /**
     * Indique si ce modèle est au format HTML.
     *
     * <p><b>Exemple :</b> {@code CONTENU_GENERAL.isHtml()} retourne {@code true} ;
     * {@code LIBELLE_REFERENCE.isHtml()} retourne {@code false}.</p>
     *
     * @return {@code true} si le modèle est HTML, {@code false} s'il est texte brut
     */
    public boolean isHtml() {
        return html;
    }

    /**
     * Retourne le libellé d'affichage dans l'interface utilisateur.
     *
     * <p><b>Exemple :</b> {@code CONTENU_GENERAL.getLibelleUi()} retourne « Lettre — général ».</p>
     *
     * @return le libellé d'affichage
     */
    public String getLibelleUi() {
        return libelleUi;
    }

    /**
     * Retourne le chemin de la ressource par défaut (classpath, sans slash initial).
     *
     * <p><b>Exemple :</b> {@code CONTENU_IA.getCheminRessource()} retourne
     * {@code "modeles/defaut/CONTENU_IA.html"}.</p>
     *
     * @return le chemin classpath de la ressource par défaut
     */
    public String getCheminRessource() {
        return cheminRessource;
    }

    /**
     * Retourne la liste des variables substituables dans ce modèle.
     *
     * <p><b>Exemple :</b> {@code LIBELLE_REFERENCE.getVariables()} retourne
     * {@code "{{REFERENCE}}, {{NOM}}, {{POSTE}}"}.</p>
     *
     * @return les variables substituables, séparées par des virgules
     */
    public String getVariables() {
        return variables;
    }

    /**
     * Retourne la clé de contenu correspondant à un code de type de contenu.
     *
     * <p><b>Exemple :</b> {@code pourTypeContenu(1)} retourne CONTENU_MICROSERVICES ;
     * {@code pourTypeContenu(null)} retourne CONTENU_GENERAL.</p>
     *
     * @param code code de {@link TypeContenu} (peut être nul)
     * @return la clé de contenu, CONTENU_GENERAL par défaut
     */
    public static CleModele pourTypeContenu(Integer code) {
        if (code != null && code == TypeContenu.MICROSERVICES.getCode()) {
            return CONTENU_MICROSERVICES;
        }
        if (code != null && code == TypeContenu.IA.getCode()) {
            return CONTENU_IA;
        }
        return CONTENU_GENERAL;
    }

    /**
     * Retourne la clé de libellé correspondant à un code de type d'annonce.
     *
     * <p><b>Exemple :</b> {@code pourTypeAnnonce(1)} retourne LIBELLE_REFERENCE ;
     * tout autre code (ou null) retourne LIBELLE_SPONTANEE.</p>
     *
     * @param code code de {@link TypeAnnonce} (peut être nul)
     * @return la clé de libellé
     */
    public static CleModele pourTypeAnnonce(Integer code) {
        return (code != null && code == TypeAnnonce.REFERENCE.getCode())
                ? LIBELLE_REFERENCE
                : LIBELLE_SPONTANEE;
    }
}
