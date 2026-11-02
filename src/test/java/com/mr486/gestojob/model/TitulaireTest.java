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

    // Ces noms sont stockés tels quels en base (colonne texte) : les renommer casserait les données.
    @Test
    void name_estFigePourLaBase() {
        assertEquals("LAURENT", Titulaire.LAURENT.name(), "valeur stockée de LAURENT");
        assertEquals("RENEE", Titulaire.RENEE.name(), "valeur stockée de RENEE");
    }
}
