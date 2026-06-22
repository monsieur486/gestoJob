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
}
