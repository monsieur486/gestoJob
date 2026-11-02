package com.mr486.gestojob.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

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
    }

    @Test
    void getCheminRessource_dependDuTitulaire() {
        assertThat(CleModele.CONTENU_IA.getCheminRessource(Titulaire.RENEE))
                .isEqualTo("modeles/defaut/RENEE/CONTENU_IA.html");
        assertThat(CleModele.LIBELLE_SPONTANEE.getCheminRessource(Titulaire.LAURENT))
                .isEqualTo("modeles/defaut/LAURENT/LIBELLE_SPONTANEE.txt");
    }

    @Test
    void ressourcesParDefaut_existentPourChaqueTitulaireEtChaqueCle() {
        for (Titulaire titulaire : Titulaire.values()) {
            for (CleModele cle : CleModele.values()) {
                String chemin = cle.getCheminRessource(titulaire);
                assertThat(Thread.currentThread().getContextClassLoader().getResource(chemin))
                        .as(chemin).isNotNull();
            }
        }
    }

    @Test
    void ressourcesRenee_neContiennentAucunTexteDeLaurent() throws IOException {
        for (CleModele cle : CleModele.values()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(cle.getCheminRessource(Titulaire.RENEE))) {
                String texte = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(texte).as(cle.name())
                        .doesNotContain("Laurent").doesNotContain("Java").doesNotContain("OpenClassrooms");
            }
        }
    }
}
