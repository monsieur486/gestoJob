package com.mr486.gestojob.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le mapping code numérique → libellé (avec icône) des statuts
 * d'annonce. Les libellés doivent être strictement identiques à ceux
 * historiquement produits, car certaines vues les comparent textuellement.
 */
class StatutAnnonceTest {

    @ParameterizedTest
    @CsvSource({
            "1,Boîte d'envoi",
            "2,En cours",
            "3,Dépassé",
            "4,Négatif",
            "5,Positif",
            "6,Archivé"
    })
    void libelle_contientLeTexteAttendu(int code, String texteAttendu) {
        assertThat(StatutAnnonce.libelle(code)).contains(texteAttendu);
    }

    @Test
    void libelle_exactementIdentiqueAuxChainesHistoriques() {
        assertThat(StatutAnnonce.libelle(2)).isEqualTo("🟠 En cours");
        assertThat(StatutAnnonce.libelle(5)).isEqualTo("🟢 Positif");
    }

    @Test
    void libelle_inconnu_pourCodeNonGere() {
        assertThat(StatutAnnonce.libelle(99)).isEqualTo("Inconnu");
    }

    @Test
    void libelle_inconnu_pourNull() {
        assertThat(StatutAnnonce.libelle(null)).isEqualTo("Inconnu");
    }
}
