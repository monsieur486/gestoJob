package com.mr486.gestojob.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Vérifie le libellé du motif d'écart exposé à la vue par l'entité.
 */
class OffreConsulteeTest {

    @Test
    void getMotifLibelle_renvoieLeLibelleDuCode() {
        assertThat(OffreConsultee.builder().motifEcart(1).build().getMotifLibelle()).isEqualTo("Profil");
        assertThat(OffreConsultee.builder().motifEcart(null).build().getMotifLibelle()).isEqualTo("Inconnu");
    }
}
