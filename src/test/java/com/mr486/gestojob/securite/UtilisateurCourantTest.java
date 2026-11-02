package com.mr486.gestojob.securite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mr486.gestojob.model.Titulaire;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class UtilisateurCourantTest {

    private final UtilisateurCourant utilisateurCourant = new UtilisateurCourant();

    @AfterEach
    void viderContexte() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(String... autorites) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("u", "p", autorites));
    }

    @Test
    void titulaire_renvoieRenee_pourAutoriteDonneesRenee() {
        connecter("ROLE_TITULAIRE", "DONNEES_RENEE");
        assertEquals(Titulaire.RENEE, utilisateurCourant.titulaire(), "titulaire attendu : Renée");
    }

    @Test
    void titulaire_renvoieLaurent_pourAutoriteDonneesLaurent() {
        connecter("ROLE_LECTEUR", "DONNEES_LAURENT");
        assertEquals(Titulaire.LAURENT, utilisateurCourant.titulaire(), "titulaire attendu : Laurent");
    }

    @Test
    void titulaire_leveIllegalState_sansAuthentification() {
        assertThrows(IllegalStateException.class, utilisateurCourant::titulaire);
    }

    @Test
    void titulaire_leveIllegalState_sansAutoriteDonnees() {
        connecter("ROLE_TITULAIRE");
        assertThrows(IllegalStateException.class, utilisateurCourant::titulaire);
    }

    @Test
    void titulaire_leveIllegalState_pourAutoriteDonneesInconnue() {
        connecter("DONNEES_INCONNU");
        assertThrows(IllegalStateException.class, utilisateurCourant::titulaire);
    }

    @Test
    void estLecteur_vrai_pourRoleLecteur() {
        connecter("ROLE_LECTEUR", "DONNEES_LAURENT");
        assertTrue(utilisateurCourant.estLecteur(), "le rôle LECTEUR doit être reconnu");
    }

    @Test
    void estLecteur_faux_pourTitulaire() {
        connecter("ROLE_TITULAIRE", "DONNEES_LAURENT");
        assertFalse(utilisateurCourant.estLecteur(), "pas de rôle LECTEUR");
    }

    @Test
    void estLecteur_faux_sansAuthentification() {
        assertFalse(utilisateurCourant.estLecteur(), "pas de rôle LECTEUR");
    }
}
