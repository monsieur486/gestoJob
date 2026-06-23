package com.mr486.gestojob.model;

/**
 * Statut de suivi d'une annonce (candidature), associant le code numérique
 * persisté en base à son libellé lisible (icône comprise).
 * <p>
 * Remplace l'ancien {@code switch} sur entiers magiques : ajouter un statut
 * se fait désormais en ajoutant une constante (extension), sans modifier la
 * logique d'affichage (principe ouvert/fermé).
 */
public enum StatutAnnonce {

    BOITE_ENVOI(1, "🖂 Boîte d'envoi"),
    EN_COURS(2, "🟠 En cours"),
    DEPASSE(3, "⏳ Dépassé"),
    NEGATIF(4, "🔴 Négatif"),
    POSITIF(5, "🟢 Positif"),
    ARCHIVE(6, "📦 Archivé");

    /** Libellé retourné lorsqu'aucun statut connu ne correspond au code. */
    public static final String LIBELLE_INCONNU = "Inconnu";

    private final int code;
    private final String libelle;

    StatutAnnonce(int code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    /**
     * Retourne le code numérique persisté en base pour ce statut.
     *
     * <p><b>Exemple :</b> {@code POSITIF.getCode()} retourne 5.</p>
     *
     * @return le code numérique du statut
     */
    public int getCode() {
        return code;
    }

    /**
     * Retourne le libellé lisible (avec icône) de ce statut.
     *
     * <p><b>Exemple :</b> {@code POSITIF.getLibelle()} retourne « 🟢 Positif ».</p>
     *
     * @return le libellé du statut
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Retourne le libellé lisible correspondant à un code de statut, ou
     * {@value #LIBELLE_INCONNU} si le code est nul ou non reconnu.
     *
     * <p><b>Exemple :</b> {@code libelle(5)} retourne « 🟢 Positif » ; {@code libelle(99)} et {@code libelle(null)} retournent « Inconnu ».</p>
     *
     * @param code le code numérique du statut (peut être nul)
     * @return le libellé correspondant, ou {@value #LIBELLE_INCONNU} si inconnu
     */
    public static String libelle(Integer code) {
        if (code != null) {
            for (StatutAnnonce statut : values()) {
                if (statut.code == code) {
                    return statut.libelle;
                }
            }
        }
        return LIBELLE_INCONNU;
    }
}
