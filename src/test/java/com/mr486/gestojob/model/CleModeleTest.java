package com.mr486.gestojob.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CleModeleTest {

    @Test
    void pourTypeContenu_mappeLesCodes() {
        assertThat(CleModele.pourTypeContenu(1)).isEqualTo(CleModele.CONTENU_MICROSERVICES);
        assertThat(CleModele.pourTypeContenu(2)).isEqualTo(CleModele.CONTENU_IA);
        assertThat(CleModele.pourTypeContenu(0)).isEqualTo(CleModele.CONTENU_GENERAL);
        assertThat(CleModele.pourTypeContenu(null)).isEqualTo(CleModele.CONTENU_GENERAL);
        assertThat(CleModele.pourTypeContenu(99)).isEqualTo(CleModele.CONTENU_GENERAL);
    }

    @Test
    void pourTypeAnnonce_referenceSinonSpontanee() {
        assertThat(CleModele.pourTypeAnnonce(1)).isEqualTo(CleModele.LIBELLE_REFERENCE);
        assertThat(CleModele.pourTypeAnnonce(0)).isEqualTo(CleModele.LIBELLE_SPONTANEE);
        assertThat(CleModele.pourTypeAnnonce(null)).isEqualTo(CleModele.LIBELLE_SPONTANEE);
    }

    @Test
    void metadonnees_sontCoherentes() {
        assertThat(CleModele.CONTENU_GENERAL.isHtml()).isTrue();
        assertThat(CleModele.CONTENU_GENERAL.getCategorie()).isEqualTo(CleModele.Categorie.CONTENU);
        assertThat(CleModele.LIBELLE_REFERENCE.isHtml()).isFalse();
        assertThat(CleModele.LIBELLE_REFERENCE.getCategorie()).isEqualTo(CleModele.Categorie.LIBELLE);
        assertThat(CleModele.CONTENU_IA.getCheminRessource()).isEqualTo("modeles/defaut/CONTENU_IA.html");
    }
}
