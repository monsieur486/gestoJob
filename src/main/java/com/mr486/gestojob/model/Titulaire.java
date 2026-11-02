package com.mr486.gestojob.model;

/**
 * Titulaire d'un compte : personne à qui appartiennent les entreprises, contacts,
 * annonces et modèles d'email. Persisté sous forme de texte (nom de la constante).
 */
public enum Titulaire {

    LAURENT("Laurent"),
    RENEE("Renée");

    private final String affichage;

    Titulaire(String affichage) {
        this.affichage = affichage;
    }

    /**
     * Retourne le prénom à afficher pour ce titulaire.
     *
     * <p><b>Exemple :</b> {@code RENEE.getAffichage()} retourne « Renée ».</p>
     *
     * @return le prénom accentué du titulaire
     */
    public String getAffichage() {
        return affichage;
    }
}
