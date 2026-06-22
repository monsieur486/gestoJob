package com.mr486.gestojob.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactTest {

    @Test
    void getMessageDePolitesse_madame_siFormule2() {
        Contact c = Contact.builder().formuleDePolistesse(2).contact("Durand").build();
        assertThat(c.getMessageDePolitesse()).isEqualTo("Madame Durand,");
    }

    @Test
    void getMessageDePolitesse_monsieur_siFormule1() {
        Contact c = Contact.builder().formuleDePolistesse(1).contact("Durand").build();
        assertThat(c.getMessageDePolitesse()).isEqualTo("Monsieur Durand,");
    }

    @Test
    void getMessageDePolitesse_generique_siAutreFormule() {
        Contact c = Contact.builder().formuleDePolistesse(0).contact("Durand").build();
        assertThat(c.getMessageDePolitesse()).isEqualTo("Madame, Monsieur,");
    }
}
