package com.mr486.gestojob.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.ProfilForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.service.ModeleEmailService;
import com.mr486.gestojob.service.ProfilService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class ParametresPageControllerTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    @Mock
    private ProfilService profilService;

    @InjectMocks
    private ParametresPageController controller;

    @Test
    void parametres_alimenteLesModeles() {
        when(modeleEmailService.listerModeles()).thenReturn(List.of());
        when(profilService.getProfilCourant()).thenReturn(new Profil());
        Model model = new ExtendedModelMap();

        String vue = controller.parametresView(model);

        assertThat(vue).isEqualTo("parametres");
        assertThat(model.getAttribute("modeles")).isNotNull();
        assertThat(model.getAttribute("page_active")).isEqualTo("parametres");
    }

    @Test
    void enregistrer_metAJour_etRedirige() {
        String vue = controller.enregistrerModele("CONTENU_GENERAL", "<p>x</p>",
                new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("redirect:/parametres");
        verify(modeleEmailService).mettreAJour("CONTENU_GENERAL", "<p>x</p>");
    }

    @Test
    void enregistrer_exposeLErreur_siServiceLeve() {
        doThrow(new MetierException("boum")).when(modeleEmailService)
                .mettreAJour("CONTENU_GENERAL", "x");
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String vue = controller.enregistrerModele("CONTENU_GENERAL", "x", redirect);

        assertThat(vue).isEqualTo("redirect:/parametres");
        assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo("boum");
    }

    @Test
    void reinitialiser_appelleLeService_etRedirige() {
        String vue = controller.reinitialiserModele("CONTENU_IA", new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("redirect:/parametres");
        verify(modeleEmailService).reinitialiser("CONTENU_IA");
    }

    @Test
    void parametresView_ajouteLeProfilCourant() {
        Profil profil = new Profil();
        profil.setCandidatNom("Renée Test");
        when(modeleEmailService.listerModeles()).thenReturn(List.of());
        when(profilService.getProfilCourant()).thenReturn(profil);
        Model model = new ExtendedModelMap();

        controller.parametresView(model);

        assertThat(model.getAttribute("profilForm")).isInstanceOf(ProfilForm.class);
        assertThat(((ProfilForm) model.getAttribute("profilForm")).getCandidatNom())
                .isEqualTo("Renée Test");
    }

    @Test
    void enregistrerProfil_valide_appelleMettreAJourEtRedirige() {
        ProfilForm form = new ProfilForm();
        BindingResult resultat = new BindException(form, "profilForm");
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String vue = controller.enregistrerProfil(form, resultat, new ExtendedModelMap(), redirect);

        assertThat(vue).isEqualTo("redirect:/parametres");
        verify(profilService).mettreAJour(form);
        assertThat(redirect.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void enregistrerProfil_invalide_reafficheSansEnregistrer() {
        ProfilForm form = new ProfilForm();
        BindingResult resultat = new BindException(form, "profilForm");
        resultat.rejectValue("candidatNom", "vide", "obligatoire");
        when(modeleEmailService.listerModeles()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String vue = controller.enregistrerProfil(form, resultat, model, new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("parametres");
        verify(profilService, never()).mettreAJour(any());
        assertThat(model.getAttribute("modeles")).isNotNull();
    }

    @Test
    void enregistrerProfil_erreurMetier_exposeLErreur() {
        ProfilForm form = new ProfilForm();
        BindingResult resultat = new BindException(form, "profilForm");
        doThrow(new MetierException("boum")).when(profilService).mettreAJour(form);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        String vue = controller.enregistrerProfil(form, resultat, new ExtendedModelMap(), redirect);

        assertThat(vue).isEqualTo("redirect:/parametres");
        assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo("boum");
    }
}
