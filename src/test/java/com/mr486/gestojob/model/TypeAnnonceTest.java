package com.mr486.gestojob.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le mapping code numérique → libellé court (avec icône) du type
 * d'annonce. Toute valeur autre que 0 (spontanée) est traitée comme une
 * candidature à une référence, conformément au comportement historique.
 */
class TypeAnnonceTest {

    @Test
    void libelleCourt_spontanee_pourCode0() {
        assertThat(TypeAnnonce.libelleCourt(0)).isEqualTo("🆓 S");
    }

    @Test
    void libelleCourt_reference_pourCode1() {
        assertThat(TypeAnnonce.libelleCourt(1)).isEqualTo("📝 A");
    }

    @Test
    void libelleCourt_reference_parDefaut_pourNull() {
        assertThat(TypeAnnonce.libelleCourt(null)).isEqualTo("📝 A");
    }
}
