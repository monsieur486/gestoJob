package com.mr486.gestojob.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContenuServiceTest {

    private final ContenuService contenuService = new ContenuService(new HtmlConverterService());

    @Test
    void html_echappeLePosteSaisiParUtilisateur() {
        String html = contenuService.getHtmlContenu(
                "<script>alert(1)</script>", 0, "Madame, Monsieur,");

        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).doesNotContain("<script>");
    }

    @Test
    void html_echappeLaFormuleDePolitesse() {
        String html = contenuService.getHtmlContenu(
                "Développeur", 0, "Madame <b>Durand</b>,");

        assertThat(html).contains("Madame &lt;b&gt;Durand&lt;/b&gt;,");
        assertThat(html).doesNotContain("<b>Durand</b>");
    }

    @Test
    void texte_nEchappePas_carCeNestPasDuHtml() {
        String txt = contenuService.getTextContenu(
                "<b>x</b>", 0, "Madame, Monsieur,");

        assertThat(txt).contains("<b>x</b>");
        assertThat(txt).doesNotContain("&lt;");
    }

    @Test
    void html_utiliseLePosteParDefaut_siVide() {
        String html = contenuService.getHtmlContenu("", 0, "Madame, Monsieur,");

        // valeur par défaut littérale, déjà encodée, non ré-échappée
        assertThat(html).contains("de d&eacute;veloppeur Java");
    }

    @Test
    void html_templateMicroservice_siTypeContenu1() {
        String html = contenuService.getHtmlContenu("Développeur", 1, "Madame, Monsieur,");

        assertThat(html).contains("microservices");
    }

    @Test
    void texte_templateMicroservice_siTypeContenu1() {
        String txt = contenuService.getTextContenu("Développeur", 1, "Madame, Monsieur,");

        assertThat(txt).contains("microservices");
    }

    @Test
    void html_templateIaAgentique_siTypeContenu2() {
        String html = contenuService.getHtmlContenu("Développeur", 2, "Madame, Monsieur,");

        assertThat(html).contains("IA agentique");
    }

    @Test
    void texte_templateIaAgentique_siTypeContenu2() {
        String txt = contenuService.getTextContenu("Développeur", 2, "Madame, Monsieur,");

        assertThat(txt).contains("agents IA");
    }

    @Test
    void texte_utiliseLePosteParDefaut_siVide() {
        String txt = contenuService.getTextContenu("", 0, "Madame, Monsieur,");

        assertThat(txt).contains("de développeur Java");
    }

    @Test
    void texte_typeContenuNull_utiliseLeTemplateGeneral_sansNpe() {
        String txt = contenuService.getTextContenu("", null, "Madame, Monsieur,");

        assertThat(txt).contains("de développeur Java");
    }

    @Test
    void html_typeContenuNull_utiliseLeTemplateGeneral_sansNpe() {
        String html = contenuService.getHtmlContenu("", null, "Madame, Monsieur,");

        assertThat(html).contains("de d&eacute;veloppeur Java");
    }

    @Test
    void texte_politesseNull_utiliseLaSalutationGenerique_sansNpe() {
        String txt = contenuService.getTextContenu("Développeur", 0, null);

        assertThat(txt).contains("Madame, Monsieur,");
    }
}
