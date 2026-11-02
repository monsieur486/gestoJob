package com.mr486.gestojob.controller;

import com.mr486.gestojob.securite.UtilisateurCourant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose à toutes les vues le nom du titulaire dont les données sont consultées
 * (affiché dans le menu).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class TitulaireConseil {

    private final UtilisateurCourant utilisateurCourant;

    /**
     * Retourne le libellé du titulaire consulté, ou {@code null} si personne n'est
     * connecté (page de connexion).
     *
     * <p><b>Exemple :</b> pour un lecteur France Travail consultant les données de
     * Laurent, retourne « Laurent » ; sur /login, retourne {@code null}.</p>
     *
     * @return le libellé du titulaire, ou {@code null} sans utilisateur authentifié
     */
    @ModelAttribute("titulaireAffichage")
    public String titulaireAffichage() {
        try {
            return utilisateurCourant.titulaire().getAffichage();
        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
