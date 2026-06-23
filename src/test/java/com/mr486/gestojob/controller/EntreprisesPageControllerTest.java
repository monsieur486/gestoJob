package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.service.AnnonceService;
import com.mr486.gestojob.service.ContactService;
import com.mr486.gestojob.service.EntrepriseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntreprisesPageControllerTest {

    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private ContactService contactService;
    @Mock
    private AnnonceService annonceService;
    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private EntreprisesPageController controller;

    @Test
    void entreprisesView_filtreActives_quandActiveTrue() {
        when(entrepriseService.rechercheEntrepriseActivePage(0))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 7), 0));

        Model model = new ExtendedModelMap();
        String view = controller.entreprisesView(model, 1, null, true);

        assertThat(view).isEqualTo("entreprises");
        assertThat(model.getAttribute("totalPages")).isEqualTo(0);
        verify(entrepriseService).rechercheEntrepriseActivePage(0);
        verify(entrepriseService, never()).getAllListePage(0);
    }

    @Test
    void entreprisesView_rechercheParNom_quandTextePresent() {
        when(entrepriseService.rechercheEntrepriseParNomPage("acme", 0)).thenReturn(Page.empty());

        Model model = new ExtendedModelMap();
        String view = controller.entreprisesView(model, 1, "acme", false);

        assertThat(view).isEqualTo("entreprises");
        verify(entrepriseService).rechercheEntrepriseParNomPage("acme", 0);
    }

    @Test
    void entreprisesView_pagine_quandNiActiveNiRecherche() {
        when(entrepriseService.getAllListePage(0)).thenReturn(Page.empty());

        Model model = new ExtendedModelMap();
        String view = controller.entreprisesView(model, 1, null, false);

        assertThat(view).isEqualTo("entreprises");
        assertThat(model.getAttribute("currentPage")).isEqualTo(1);
        verify(entrepriseService).getAllListePage(0);
    }

    @Test
    void activate_appelleLeService_etRedirige() {
        assertThat(controller.activateEntreprise(4, 2)).isEqualTo("redirect:/entreprises?page=2");
        verify(entrepriseService).activeEntreprise(4);
    }

    @Test
    void desactivate_appelleLeService_etRedirige() {
        assertThat(controller.desactivateEntreprise(4, 1)).isEqualTo("redirect:/entreprises?page=1");
        verify(entrepriseService).desactiveEntreprise(4);
    }

    @Test
    void ajoutEntreprise_redirigeVersLeDetail_siValide() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(entrepriseService.save(any(EntrepriseForm.class))).thenReturn(11);

        String view = controller.ajoutEntrepriseSubmit(
                EntrepriseForm.builder().nom("ACME").build(), bindingResult, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/entreprises/11");
    }

    @Test
    void ajoutEntreprise_retourneFormulaire_siErreurs() {
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = controller.ajoutEntrepriseSubmit(
                new EntrepriseForm(), bindingResult, new ExtendedModelMap());

        assertThat(view).isEqualTo("add-entreprise");
        verify(entrepriseService, never()).save(any(EntrepriseForm.class));
    }

    @Test
    void ajoutEntreprise_exposeLErreur_siServiceLeve() {
        when(bindingResult.hasErrors()).thenReturn(false);
        when(entrepriseService.save(any(EntrepriseForm.class)))
                .thenThrow(new RuntimeException("L'entreprise existe déjà"));

        Model model = new ExtendedModelMap();
        String view = controller.ajoutEntrepriseSubmit(
                EntrepriseForm.builder().nom("ACME").build(), bindingResult, model);

        assertThat(view).isEqualTo("add-entreprise");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("L'entreprise existe déjà");
    }

    @Test
    void detail_chargeEntrepriseContactsEtAnnonces() {
        when(entrepriseService.getEntreprise(8)).thenReturn(Entreprise.builder().id(8).nom("ACME").build());
        when(contactService.getAllContact(8)).thenReturn(List.of());
        when(annonceService.annoncesListeByEntrepriseId(8)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.detailEntrepriseView(8, model);

        assertThat(view).isEqualTo("entreprise_detail");
        assertThat(model.getAttribute("entreprise")).isNotNull();
        assertThat(model.getAttribute("annonceForm")).isInstanceOf(AnnonceForm.class);
    }

    @Test
    void ajoutAnnonce_fixeLEntrepriseId_etRedirige_siValide() {
        when(bindingResult.hasErrors()).thenReturn(false);
        AnnonceForm form = new AnnonceForm();

        String view = controller.ajoutAnnonceSubmit(8, form, bindingResult, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/entreprises/8");
        assertThat(form.getEntrepriseId()).isEqualTo(8);
        verify(annonceService).saveForm(form);
    }

    @Test
    void ajoutAnnonce_rechargeLeDetail_siErreurs() {
        when(bindingResult.hasErrors()).thenReturn(true);
        when(entrepriseService.getEntreprise(8)).thenReturn(Entreprise.builder().id(8).nom("ACME").build());
        when(contactService.getAllContact(8)).thenReturn(List.of());
        when(annonceService.annoncesListeByEntrepriseId(8)).thenReturn(List.of());

        String view = controller.ajoutAnnonceSubmit(8, new AnnonceForm(), bindingResult, new ExtendedModelMap());

        assertThat(view).isEqualTo("entreprise_detail");
        verify(annonceService, never()).saveForm(any(AnnonceForm.class));
    }

    @Test
    void supprimerAnnonce_supprime_etRedirigeVersLaFiche() {
        String view = controller.supprimerAnnonce(8, 7L, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/entreprises/8");
        verify(annonceService).deleteAnnonce(7L);
    }

    @Test
    void supprimerAnnonce_exposeLErreur_siServiceLeve() {
        doThrow(new RuntimeException("suppression impossible"))
                .when(annonceService).deleteAnnonce(7L);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.supprimerAnnonce(8, 7L, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/entreprises/8");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .asString().contains("suppression impossible");
    }
}
