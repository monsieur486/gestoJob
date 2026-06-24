package com.mr486.gestojob.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ContenuServiceTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    private ContenuService contenuService;

    @BeforeEach
    void setUp() {
        contenuService = new ContenuService(new HtmlConverterService(), modeleEmailService);
        lenient().when(modeleEmailService.getContenu("CONTENU_GENERAL"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} chez nous</p>");
        lenient().when(modeleEmailService.getContenu("CONTENU_MICROSERVICES"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} en microservices</p>");
        lenient().when(modeleEmailService.getContenu("CONTENU_IA"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} avec des agents IA</p>");
    }

    @Test
    void html_echappeLePosteSaisiParUtilisateur() {
        String html = contenuService.getHtmlContenu("<script>alert(1)</script>", 0, "Madame, Monsieur,");
        assertThat(html).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    void html_echappeLaFormuleDePolitesse() {
        String html = contenuService.getHtmlContenu("Développeur", 0, "Madame <b>Durand</b>,");
        assertThat(html).contains("Madame &lt;b&gt;Durand&lt;/b&gt;,").doesNotContain("<b>Durand</b>");
    }

    @Test
    void texte_nEchappePas_carCeNestPasDuHtml() {
        String txt = contenuService.getTextContenu("Dév & Co", 0, "Madame, Monsieur,");
        assertThat(txt).contains("Dév & Co").doesNotContain("&amp;");
    }

    @Test
    void html_utiliseLePosteParDefaut_siVide() {
        String html = contenuService.getHtmlContenu("", 0, "Madame, Monsieur,");
        // htmlEscape produit les entités nommées (é -> &eacute;)
        assertThat(html).contains("de d&eacute;veloppeur Java");
    }

    @Test
    void texte_utiliseLePosteParDefaut_siVide() {
        String txt = contenuService.getTextContenu("", 0, "Madame, Monsieur,");
        assertThat(txt).contains("de développeur Java");
    }

    @Test
    void html_templateMicroservice_siTypeContenu1() {
        assertThat(contenuService.getHtmlContenu("Dev", 1, "Madame, Monsieur,")).contains("microservices");
    }

    @Test
    void html_templateIaAgentique_siTypeContenu2() {
        assertThat(contenuService.getHtmlContenu("Dev", 2, "Madame, Monsieur,")).contains("agents IA");
    }

    @Test
    void texte_typeContenuNull_utiliseLeTemplateGeneral_sansNpe() {
        assertThat(contenuService.getTextContenu("", null, "Madame, Monsieur,")).contains("de développeur Java");
    }

    @Test
    void texte_politesseNull_utiliseLaSalutationGenerique_sansNpe() {
        String txt = contenuService.getTextContenu("Dev", 0, null);
        assertThat(txt).contains("Madame, Monsieur,");
    }

    @Test
    void texte_deriveDuHtml_substitueLaPolitesse() {
        String txt = contenuService.getTextContenu("Dev", 0, "Madame Durand,");
        assertThat(txt).contains("Madame Durand,").doesNotContain("{{POLITESSE}}");
    }
}
