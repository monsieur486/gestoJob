package com.mr486.gestojob.securite;

import com.mr486.gestojob.model.Titulaire;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Accès à l'utilisateur connecté : de quel titulaire il consulte les données, et
 * s'il est simple lecteur.
 *
 * <p>Le lien compte / titulaire est porté par les autorités {@code DONNEES_*}
 * accordées à la connexion (voir {@code SecurityConfig}).
 */
@Component
public class UtilisateurCourant {

    private static final String PREFIXE_DONNEES = "DONNEES_";
    private static final String ROLE_LECTEUR = "ROLE_LECTEUR";

    /**
     * Retourne le titulaire dont l'utilisateur connecté consulte les données.
     *
     * <p><b>Exemple :</b> un compte portant l'autorité {@code DONNEES_RENEE}
     * retourne {@code Titulaire.RENEE}.</p>
     *
     * @return le titulaire des données de l'utilisateur connecté
     * @throws IllegalStateException sans authentification, ou sans autorité
     *                               {@code DONNEES_*} reconnue
     */
    public Titulaire titulaire() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null) {
            throw new IllegalStateException("Aucun utilisateur authentifié");
        }
        for (GrantedAuthority autorite : authentification.getAuthorities()) {
            String nom = autorite.getAuthority();
            if (nom != null && nom.startsWith(PREFIXE_DONNEES)) {
                try {
                    return Titulaire.valueOf(nom.substring(PREFIXE_DONNEES.length()).toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Autorité de données inconnue : " + nom, e);
                }
            }
        }
        throw new IllegalStateException("Aucune autorité DONNEES_* pour l'utilisateur connecté");
    }

    /**
     * Indique si l'utilisateur connecté est un simple lecteur (consultation seule).
     *
     * @return {@code true} s'il porte le rôle {@code LECTEUR}, {@code false} sinon
     *         (y compris sans authentification)
     */
    public boolean estLecteur() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        return authentification != null && authentification.getAuthorities().stream()
                .anyMatch(autorite -> ROLE_LECTEUR.equals(autorite.getAuthority()));
    }
}
