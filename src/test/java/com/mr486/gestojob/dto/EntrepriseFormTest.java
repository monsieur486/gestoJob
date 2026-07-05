package com.mr486.gestojob.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr486.gestojob.model.Entreprise;
import org.junit.jupiter.api.Test;

class EntrepriseFormTest {

    @Test
    void appliquerA_recopieLesChampsDuFormulaireVersLEntite() {
        EntrepriseForm form = EntrepriseForm.builder()
                .nom("ACME").estActive(true).adresse1("1 rue X").adresse2("Bât. B")
                .codePostal("67000").ville("Strasbourg").build();
        Entreprise e = new Entreprise();

        form.appliquerA(e);

        assertThat(e.getNom()).isEqualTo("ACME");
        assertThat(e.getCodePostal()).isEqualTo("67000");
        assertThat(e.getVille()).isEqualTo("Strasbourg");
        assertThat(e.getEstActive()).isTrue();
    }

    @Test
    void entity_construitUneNouvelleEntiteAvecLesChampsDuFormulaire() {
        EntrepriseForm form = EntrepriseForm.builder()
                .nom("ACME").ville("Lyon").codePostal("69000").build();

        Entreprise e = EntrepriseForm.entity(form);

        assertThat(e.getNom()).isEqualTo("ACME");
        assertThat(e.getVille()).isEqualTo("Lyon");
        assertThat(e.getCodePostal()).isEqualTo("69000");
    }
}
