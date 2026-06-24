package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.ModeleEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametresPageControllerTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    @InjectMocks
    private ParametresPageController controller;

    @Test
    void parametres_alimenteLesModeles() {
        when(modeleEmailService.listerModeles()).thenReturn(List.of());
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
        doThrow(new RuntimeException("boum")).when(modeleEmailService)
                .mettreAJour("CONTENU_GENERAL", "x");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

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
}
