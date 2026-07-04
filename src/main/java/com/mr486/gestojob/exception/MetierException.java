package com.mr486.gestojob.exception;

/**
 * Exception métier signalant une erreur fonctionnelle destinée à l'utilisateur
 * (validation, conflit, règle de gestion non respectée).
 *
 * <p>Son message est rédigé en français et directement affichable : les
 * contrôleurs l'attrapent pour le transmettre en message flash, tandis que les
 * erreurs purement techniques (accès aux données, entrées/sorties) remontent
 * sans être exposées à l'utilisateur.
 *
 * <p><b>Exemple :</b> {@code throw new MetierException("L'entreprise existe déjà")} interrompt l'enregistrement et affiche ce message sur la page.
 */
public class MetierException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Construit une exception métier avec un message destiné à l'utilisateur.
     *
     * <p><b>Exemple :</b> {@code new MetierException("Veuillez renseigner un email")} porte ce libellé, réutilisé tel quel en message flash.
     *
     * @param message le message d'erreur en français, affichable à l'utilisateur
     */
    public MetierException(String message) {
        super(message);
    }

    /**
     * Construit une exception métier avec un message et la cause d'origine.
     *
     * <p><b>Exemple :</b> {@code new MetierException("Modèle inconnu : X", ex)} conserve la trace de l'exception {@code ex} sous-jacente.
     *
     * @param message le message d'erreur en français, affichable à l'utilisateur
     * @param cause   l'exception d'origine à conserver dans la trace
     */
    public MetierException(String message, Throwable cause) {
        super(message, cause);
    }
}
