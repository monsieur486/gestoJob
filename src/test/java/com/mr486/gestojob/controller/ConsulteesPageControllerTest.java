package com.mr486.gestojob.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.OffreConsulteeForm;
import com.mr486.gestojob.model.MotifEcart;
import com.mr486.gestojob.service.OffreConsulteeService;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

/**
 * Vérifie la page du journal des offres consultées : remplissage du modèle,
 * saisie (valide ou en erreur) et suppression.
 */
@ExtendWith(MockitoExtension.class)
class ConsulteesPageControllerTest {

    @Mock
    private OffreConsulteeService offreConsulteeService;
    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ConsulteesPageController controller;

    @Test
    void liste_remplitLeModele() {
        when(offreConsulteeService.lister(0)).thenReturn(Page.empty());

        Model model = new ExtendedModelMap();
        String vue = controller.liste(model, 1);

        assertThat(vue).isEqualTo("consultees");
        assertThat(model.getAttribute("page_active")).isEqualTo("consultees");
        assertThat(model.getAttribute("currentPage")).isEqualTo(1);
        assertThat(model.getAttribute("motifs")).isEqualTo(MotifEcart.values());
        assertThat(((OffreConsulteeForm) model.getAttribute("form")).getDateConsultation())
                .isEqualTo(LocalDate.now());
        verify(offreConsulteeService).lister(0);
    }

    @Test
    void ajouter_valide_enregistreEtRedirige() {
        OffreConsulteeForm form = new OffreConsulteeForm();
        when(bindingResult.hasErrors()).thenReturn(false);

        assertThat(controller.ajouter(form, bindingResult, new ExtendedModelMap()))
                .isEqualTo("redirect:/consultees");
        verify(offreConsulteeService).ajouter(form);
    }

    @Test
    void ajouter_invalide_reafficheSansEnregistrer() {
        OffreConsulteeForm form = new OffreConsulteeForm();
        form.setIntitule("saisie à conserver");
        when(bindingResult.hasErrors()).thenReturn(true);
        when(offreConsulteeService.lister(anyInt())).thenReturn(Page.empty());
        Model model = new ExtendedModelMap();
        model.addAttribute("form", form);

        assertThat(controller.ajouter(form, bindingResult, model)).isEqualTo("consultees");
        verify(offreConsulteeService, never()).ajouter(any());
        assertThat(model.getAttribute("form")).isSameAs(form);
    }

    @Test
    void supprimer_redirige() {
        assertThat(controller.supprimer(5L)).isEqualTo("redirect:/consultees");
        verify(offreConsulteeService).supprimer(5L);
    }

    @Test
    void supprimer_idInconnu_propageNoSuchElement() {
        doThrow(new NoSuchElementException()).when(offreConsulteeService).supprimer(9L);

        assertThatThrownBy(() -> controller.supprimer(9L)).isInstanceOf(NoSuchElementException.class);
    }
}
