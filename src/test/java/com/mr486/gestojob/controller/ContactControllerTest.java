package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

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

        String view = controller.ajoutContactSubmit(form, 5, bindingResult, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/entreprises/5");
        verify(contactService).saveContact(form, 5);
    }

    @Test
    void ajoutContact_neSauvegardePas_siErreursDeValidation() {
        when(bindingResult.hasErrors()).thenReturn(true);
        ContactForm form = ContactForm.builder().build();

        controller.ajoutContactSubmit(form, 5, bindingResult, new ExtendedModelMap());

        verify(contactService, never()).saveContact(form, 5);
    }

    @Test
    void ajoutContact_exposeLeMessageDErreur_siServiceLeve() {
        when(bindingResult.hasErrors()).thenReturn(false);
        ContactForm form = ContactForm.builder().email("a@b.fr").formuleDePolitesse(0).build();
        doThrow(new RuntimeException("Veuillez renseigner un email"))
                .when(contactService).saveContact(form, 5);

        Model model = new ExtendedModelMap();
        controller.ajoutContactSubmit(form, 5, bindingResult, model);

        assertThat(model.getAttribute("errorMessage")).isEqualTo("Veuillez renseigner un email");
    }
}
