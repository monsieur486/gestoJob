package com.mr486.gestojob.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Vérifie l'affichage des titulaires. */
class TitulaireTest {

    @Test
    void getAffichage_renvoieLeNomAccentue() {
        assertEquals("Renée", Titulaire.RENEE.getAffichage(), "affichage de RENEE");
        assertEquals("Laurent", Titulaire.LAURENT.getAffichage(), "affichage de LAURENT");
    }
}
