package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;
    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ContactController controller;

    @Test
    void ajoutContact_redirigeVersLEntreprise_siValide() {
        when(bindingResult.hasErrors()).thenReturn(false);
        ContactForm form = ContactForm.builder().email("a@b.fr").formuleDePolitesse(0).build();

        String view = controller.ajoutContactSubmit(form, 5, bindingResult, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/entreprises/5");
        verify(contactService).saveContact(form, 5);
    }

    @Test
    void ajoutContact_neSauvegardePas_siErreursDeValidation() {
        when(bindingResult.hasErrors()).thenReturn(true);
        ContactForm form = ContactForm.builder().build();

        controller.ajoutContactSubmit(form, 5, bindingResult, new RedirectAttributesModelMap());

        verify(contactService, never()).saveContact(form, 5);
    }

    @Test
    void ajoutContact_exposeLeMessageDErreur_siServiceLeve() {
        when(bindingResult.hasErrors()).thenReturn(false);
        ContactForm form = ContactForm.builder().email("a@b.fr").formuleDePolitesse(0).build();
        doThrow(new RuntimeException("Veuillez renseigner un email"))
                .when(contactService).saveContact(form, 5);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.ajoutContactSubmit(form, 5, bindingResult, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/entreprises/5");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Veuillez renseigner un email");
    }

    @Test
    void supprimerContact_supprime_etRedirige() {
        String view = controller.supprimerContact(5, 9L, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/entreprises/5");
        verify(contactService).deleteContact(9L);
    }

    @Test
    void supprimerContact_exposeLErreur_siServiceLeve() {
        doThrow(new RuntimeException("Ce contact est rattaché à des annonces ; supprimez-les d'abord."))
                .when(contactService).deleteContact(9L);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.supprimerContact(5, 9L, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/entreprises/5");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).asString().contains("annonces");
    }
}
