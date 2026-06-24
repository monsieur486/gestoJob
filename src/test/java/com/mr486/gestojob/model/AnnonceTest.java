package com.mr486.gestojob.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AnnonceTest {

    @ParameterizedTest
    @CsvSource({
            "1,Boîte d'envoi",
            "2,En cours",
            "3,Dépassé",
            "4,Négatif",
            "5,Positif",
            "6,Archivé"
    })
    void getStatusAnnonceString_renvoieLeLibelleAttendu(int status, String libelleAttendu) {
        Annonce a = Annonce.builder().statusAnnonce(status).build();
        assertThat(a.getStatusAnnonceString()).contains(libelleAttendu);
    }

    @Test
    void getStatusAnnonceString_renvoieInconnu_pourStatutNonGere() {
        Annonce a = Annonce.builder().statusAnnonce(99).build();
        assertThat(a.getStatusAnnonceString()).isEqualTo("Inconnu");
    }

}
