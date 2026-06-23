package com.mr486.gestojob.model;

/**
 * Type d'une annonce, associant le code numérique persisté en base à son
 * libellé court (icône comprise) affiché dans les listes.
 * <p>
 * Remplace l'ancien {@code if/else} sur entiers magiques : par convention
 * historique, seul le code 0 désigne une candidature spontanée, toute autre
 * valeur (y compris nulle) désigne une candidature à une référence.
 */
public enum TypeAnnonce {

    SPONTANEE(0, "🆓 S"),
    REFERENCE(1, "📝 A");

    private final int code;
    private final String libelleCourt;

    TypeAnnonce(int code, String libelleCourt) {
        this.code = code;
        this.libelleCourt = libelleCourt;
    }

    /**
     * Retourne le code numérique persisté en base pour ce type.
     *
     * <p><b>Exemple :</b> {@code SPONTANEE.getCode()} retourne 0.</p>
     *
     * @return le code numérique du type
     */
    public int getCode() {
        return code;
    }

    /**
     * Retourne le libellé court (avec icône) de ce type.
     *
     * <p><b>Exemple :</b> {@code SPONTANEE.getLibelleCourt()} retourne « 🆓 S ».</p>
     *
     * @return le libellé court du type
     */
    public String getLibelleCourt() {
        return libelleCourt;
    }

    /**
     * Retourne le libellé court correspondant à un code de type. Seul le code 0
     * donne le libellé « spontanée » ; tout autre code (ou {@code null}) donne
     * le libellé « référence ».
     *
     * <p><b>Exemple :</b> {@code libelleCourt(0)} retourne « 🆓 S » ; {@code libelleCourt(1)} et {@code libelleCourt(null)} retournent « 📝 A ».</p>
     *
     * @param code le code numérique du type (peut être nul)
     * @return le libellé court correspondant
     */
    public static String libelleCourt(Integer code) {
        return (code != null && code == SPONTANEE.code)
                ? SPONTANEE.libelleCourt
                : REFERENCE.libelleCourt;
    }
}
