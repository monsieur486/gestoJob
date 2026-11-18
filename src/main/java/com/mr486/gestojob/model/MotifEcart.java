package com.mr486.gestojob.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Motif pour lequel une offre consultée a été écartée sans candidature,
 * associant le code numérique persisté en base à son libellé lisible.
 *
 * <p><b>Exemple :</b> une offre exigeant cinq ans de Kotlin est écartée avec
 * le motif {@link #PROFIL} (code 1) ; une offre à Lyon pour un candidat
 * nantais, avec le motif {@link #DISTANCE} (code 3).</p>
 */
public enum MotifEcart {

    /** Le profil recherché ne correspond pas au candidat. */
    PROFIL(1, "Profil"),
    /** Le poste ou la mission ne convient pas au candidat. */
    POSTE(2, "Poste"),
    /** Le lieu de travail est trop éloigné. */
    DISTANCE(3, "Distance");

    /** Libellé retourné lorsqu'aucun motif connu ne correspond au code. */
    public static final String LIBELLE_INCONNU = "Inconnu";

    private final int code;
    private final String libelle;

    MotifEcart(int code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    /**
     * Retourne le code numérique persisté en base.
     *
     * <p><b>Exemple :</b> {@code MotifEcart.POSTE.getCode()} renvoie 2.</p>
     *
     * @return le code du motif
     */
    public int getCode() {
        return code;
    }

    /**
     * Retourne le libellé lisible du motif.
     *
     * <p><b>Exemple :</b> {@code MotifEcart.POSTE.getLibelle()} renvoie « Poste ».</p>
     *
     * @return le libellé du motif
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Retourne le libellé correspondant à un code, ou {@link #LIBELLE_INCONNU}.
     *
     * <p><b>Exemple :</b> {@code libelle(3)} renvoie « Distance », {@code libelle(null)} « Inconnu ».</p>
     *
     * @param code le code du motif, éventuellement {@code null}
     * @return le libellé du motif, jamais {@code null}
     */
    public static String libelle(Integer code) {
        return depuisCode(code).map(MotifEcart::getLibelle).orElse(LIBELLE_INCONNU);
    }

    /**
     * Retrouve le motif correspondant à un code.
     *
     * <p><b>Exemple :</b> {@code depuisCode(1)} renvoie {@code PROFIL}, {@code depuisCode(0)} un optionnel vide.</p>
     *
     * @param code le code du motif, éventuellement {@code null}
     * @return le motif, ou un optionnel vide si le code est nul ou inconnu
     */
    public static Optional<MotifEcart> depuisCode(Integer code) {
        if (code != null) {
            for (MotifEcart motif : values()) {
                if (motif.code == code) {
                    return Optional.of(motif);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrouve le motif correspondant à un nom, sans tenir compte de la casse
     * ni des espaces autour.
     *
     * <p><b>Exemple :</b> {@code depuisNom(" distance ")} renvoie {@code DISTANCE},
     * {@code depuisNom("autre")} un optionnel vide.</p>
     *
     * @param nom le nom du motif, éventuellement {@code null} ou vide
     * @return le motif, ou un optionnel vide si le nom est absent ou inconnu
     */
    public static Optional<MotifEcart> depuisNom(String nom) {
        if (nom == null || nom.isBlank()) {
            return Optional.empty();
        }
        String nomNormalise = nom.strip().toUpperCase(Locale.ROOT);
        for (MotifEcart motif : values()) {
            if (motif.name().equals(nomNormalise)) {
                return Optional.of(motif);
            }
        }
        return Optional.empty();
    }
}
