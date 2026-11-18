package com.mr486.gestojob.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Vérifie la correspondance code / nom / libellé des motifs d'écart d'une
 * offre consultée, et le comportement sur les valeurs inconnues (venues d'un
 * formulaire ou de l'API).
 */
class MotifEcartTest {

    @Test
    void libelle_codeConnu() {
        assertThat(MotifEcart.libelle(1)).isEqualTo("Profil");
        assertThat(MotifEcart.libelle(2)).isEqualTo("Poste");
        assertThat(MotifEcart.libelle(3)).isEqualTo("Distance");
    }

    @Test
    void libelle_nullOuInconnu() {
        assertThat(MotifEcart.libelle(null)).isEqualTo("Inconnu");
        assertThat(MotifEcart.libelle(9)).isEqualTo("Inconnu");
    }

    @Test
    void depuisCode_valide_etInvalide() {
        assertThat(MotifEcart.depuisCode(3)).contains(MotifEcart.DISTANCE);
        assertThat(MotifEcart.depuisCode(0)).isEmpty();
        assertThat(MotifEcart.depuisCode(null)).isEmpty();
    }

    @Test
    void depuisNom_insensibleALaCasse() {
        assertThat(MotifEcart.depuisNom("profil")).contains(MotifEcart.PROFIL);
        assertThat(MotifEcart.depuisNom(" Poste ")).contains(MotifEcart.POSTE);
        assertThat(MotifEcart.depuisNom("autre")).isEmpty();
        assertThat(MotifEcart.depuisNom(null)).isEmpty();
        assertThat(MotifEcart.depuisNom(" ")).isEmpty();
    }

    @Test
    void getCode_etGetLibelle() {
        assertThat(MotifEcart.POSTE.getCode()).isEqualTo(2);
        assertThat(MotifEcart.POSTE.getLibelle()).isEqualTo("Poste");
    }
}
