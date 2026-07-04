package com.mr486.gestojob.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Vérifie que l'exception métier transporte fidèlement son message destiné à
 * l'utilisateur et, le cas échéant, la cause d'origine.
 */
class MetierExceptionTest {

    @Test
    void message_estConserve() {
        MetierException ex = new MetierException("L'entreprise existe déjà");

        assertThat(ex.getMessage()).isEqualTo("L'entreprise existe déjà");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void messageEtCause_sontConserves() {
        IllegalArgumentException cause = new IllegalArgumentException("clé invalide");

        MetierException ex = new MetierException("Modèle inconnu : X", cause);

        assertThat(ex.getMessage()).isEqualTo("Modèle inconnu : X");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
