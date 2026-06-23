package com.mr486.gestojob.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactTest {

    @Test
    void getMessageDePolitesse_madame_siFormule2() {
        Contact c = Contact.builder().formuleDePolitesse(2).nom("Durand").build();
        assertThat(c.getMessageDePolitesse()).isEqualTo("Madame Durand,");
    }

    @Test
    void getMessageDePolitesse_monsieur_siFormule1() {
        Contact c = Contact.builder().formuleDePolitesse(1).nom("Durand").build();
        assertThat(c.getMessageDePolitesse()).isEqualTo("Monsieur Durand,");
    }

    @Test
    void getMessageDePolitesse_generique_siAutreFormule() {
        Contact c = Contact.builder().formuleDePolitesse(0).nom("Durand").build();
        assertThat(c.getMessageDePolitesse()).isEqualTo("Madame, Monsieur,");
    }
}
