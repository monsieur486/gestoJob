package com.mr486.gestojob.model;

/**
 * Type de contenu (modèle de lettre) d'une annonce, associant le code numérique
 * persisté en base au libellé court affiché dans les listes.
 * <p>
 * Remplace les entiers magiques (1 = microservices, 2 = IA agentique, sinon
 * généraliste) : ajouter un modèle de lettre se fait en ajoutant une constante,
 * sans modifier la logique de sélection (principe ouvert/fermé).
 */
public enum TypeContenu {

    GENERAL(0, "G"),
    MICROSERVICES(1, "MS"),
    IA(2, "IA");

    private final int code;
    private final String libelleCourt;

    TypeContenu(int code, String libelleCourt) {
        this.code = code;
        this.libelleCourt = libelleCourt;
    }

    /**
     * Retourne le code numérique persisté en base pour ce type de contenu.
     *
     * <p><b>Exemple :</b> {@code MICROSERVICES.getCode()} retourne 1.</p>
     *
     * @return le code numérique du type de contenu
     */
    public int getCode() {
        return code;
    }

    /**
     * Retourne le libellé court (étiquette de liste) de ce type de contenu.
     *
     * <p><b>Exemple :</b> {@code IA.getLibelleCourt()} retourne « IA ».</p>
     *
     * @return le libellé court du type de contenu
     */
    public String getLibelleCourt() {
        return libelleCourt;
    }

    /**
     * Retourne le libellé court correspondant à un code de type de contenu, ou
     * celui du type généraliste si le code est nul ou non reconnu.
     *
     * <p><b>Exemple :</b> {@code libelleCourt(1)} retourne « MS » ; {@code libelleCourt(null)} et {@code libelleCourt(9)} retournent « G ».</p>
     *
     * @param code le code numérique du type de contenu (peut être nul)
     * @return le libellé court correspondant, ou « G » si inconnu
     */
    public static String libelleCourt(Integer code) {
        if (code != null) {
            for (TypeContenu type : values()) {
                if (type.code == code) {
                    return type.libelleCourt;
                }
            }
        }
        return GENERAL.libelleCourt;
    }
}
