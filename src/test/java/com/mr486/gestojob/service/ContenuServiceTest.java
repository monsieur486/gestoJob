package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContenuServiceTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    @Mock
    private ProfilService profilService;

    private ContenuService contenuService;

    @BeforeEach
    void setUp() {
        contenuService = new ContenuService(new HtmlConverterService(), modeleEmailService, profilService);
        lenient().when(modeleEmailService.getContenu(Titulaire.LAURENT, "CONTENU_GENERAL"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} chez nous</p>");
        lenient().when(modeleEmailService.getContenu(Titulaire.LAURENT, "CONTENU_MICROSERVICES"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} en microservices</p>");
        lenient().when(modeleEmailService.getContenu(Titulaire.LAURENT, "CONTENU_IA"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} avec des agents IA</p>");
        Profil laurent = Profil.builder().titulaire(Titulaire.LAURENT).build();
        lenient().when(profilService.getProfil(Titulaire.LAURENT)).thenReturn(laurent);
        lenient().when(profilService.posteParDefaut(laurent, CleModele.CONTENU_GENERAL))
                .thenReturn("de développeur Java");
        lenient().when(profilService.posteParDefaut(laurent, CleModele.CONTENU_MICROSERVICES))
                .thenReturn("de développeur Java orienté microservices");
        lenient().when(profilService.posteParDefaut(laurent, CleModele.CONTENU_IA))
                .thenReturn("de développeur Java back-end orienté IA agentique");
    }

    @Test
    void posteVide_utiliseLePosteParDefautDuProfil() {
        Profil renee = Profil.builder().titulaire(Titulaire.RENEE).build();
        when(profilService.getProfil(Titulaire.RENEE)).thenReturn(renee);
        when(profilService.posteParDefaut(renee, CleModele.CONTENU_MICROSERVICES))
                .thenReturn("correspondant à mon profil");
        when(modeleEmailService.getContenu(Titulaire.RENEE, "CONTENU_MICROSERVICES"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}}</p>");

        String html = contenuService.getHtmlContenu(Titulaire.RENEE, "", 1, "Madame, Monsieur,");

        // htmlEscape encode « à » en entité : rendu identique dans le courriel.
        assertThat(html).contains("correspondant &agrave; mon profil");
    }

    @Test
    void html_echappeLePosteSaisiParUtilisateur() {
        String html = contenuService.getHtmlContenu(
                Titulaire.LAURENT, "<script>alert(1)</script>", 0, "Madame, Monsieur,");
        assertThat(html).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    void html_echappeLaFormuleDePolitesse() {
        String html = contenuService.getHtmlContenu(Titulaire.LAURENT, "Développeur", 0, "Madame <b>Durand</b>,");
        assertThat(html).contains("Madame &lt;b&gt;Durand&lt;/b&gt;,").doesNotContain("<b>Durand</b>");
    }

    @Test
    void texte_nEchappePas_carCeNestPasDuHtml() {
        String txt = contenuService.getTextContenu(Titulaire.LAURENT, "Dév & Co", 0, "Madame, Monsieur,");
        assertThat(txt).contains("Dév & Co").doesNotContain("&amp;");
    }

    @Test
    void html_utiliseLePosteParDefaut_siVide() {
        String html = contenuService.getHtmlContenu(Titulaire.LAURENT, "", 0, "Madame, Monsieur,");
        // htmlEscape produit les entités nommées (é -> &eacute;)
        assertThat(html).contains("de d&eacute;veloppeur Java");
    }

    @Test
    void texte_utiliseLePosteParDefaut_siVide() {
        String txt = contenuService.getTextContenu(Titulaire.LAURENT, "", 0, "Madame, Monsieur,");
        assertThat(txt).contains("de développeur Java");
    }

    @Test
    void html_templateMicroservice_siTypeContenu1() {
        assertThat(contenuService.getHtmlContenu(Titulaire.LAURENT, "Dev", 1, "Madame, Monsieur,"))
                .contains("microservices");
    }

    @Test
    void html_templateIaAgentique_siTypeContenu2() {
        assertThat(contenuService.getHtmlContenu(Titulaire.LAURENT, "Dev", 2, "Madame, Monsieur,"))
                .contains("agents IA");
    }

    @Test
    void texte_typeContenuNull_utiliseLeTemplateGeneral_sansNpe() {
        assertThat(contenuService.getTextContenu(Titulaire.LAURENT, "", null, "Madame, Monsieur,"))
                .contains("de développeur Java");
    }

    @Test
    void texte_politesseNull_utiliseLaSalutationGenerique_sansNpe() {
        String txt = contenuService.getTextContenu(Titulaire.LAURENT, "Dev", 0, null);
        assertThat(txt).contains("Madame, Monsieur,");
    }

    @Test
    void texte_deriveDuHtml_substitueLaPolitesse() {
        String txt = contenuService.getTextContenu(Titulaire.LAURENT, "Dev", 0, "Madame Durand,");
        assertThat(txt).contains("Madame Durand,").doesNotContain("{{POLITESSE}}");
    }
}
