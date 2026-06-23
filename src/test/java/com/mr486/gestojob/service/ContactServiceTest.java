package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.persistance.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;
    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private AnnonceRepository annonceRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void deleteContact_supprime_siNonRattacheADesAnnonces() {
        when(annonceRepository.existsByContactId(9L)).thenReturn(false);

        contactService.deleteContact(9L);

        verify(contactRepository).deleteById(9L);
    }

    @Test
    void deleteContact_leveUneException_siRattacheADesAnnonces() {
        when(annonceRepository.existsByContactId(9L)).thenReturn(true);

        assertThatThrownBy(() -> contactService.deleteContact(9L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("annonce");

        verify(contactRepository, never()).deleteById(any());
    }

    @Test
    void saveContact_leveUneException_siEntrepriseInconnue() {
        when(entrepriseService.existe(7)).thenReturn(false);
        ContactForm form = ContactForm.builder().email("a@b.fr").formuleDePolitesse(0).build();

        assertThatThrownBy(() -> contactService.saveContact(form, 7))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void saveContact_leveUneException_siFormuleDePolitesseSansEmail() {
        when(entrepriseService.existe(7)).thenReturn(true);
        ContactForm form = ContactForm.builder().email("").formuleDePolitesse(1).build();

        assertThatThrownBy(() -> contactService.saveContact(form, 7))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void saveContact_leveUneException_siFormuleDePolitesseEtEmailNull() {
        when(entrepriseService.existe(7)).thenReturn(true);
        ContactForm form = ContactForm.builder().email(null).formuleDePolitesse(1).build();

        assertThatThrownBy(() -> contactService.saveContact(form, 7))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void saveContact_enregistre_siFormuleNulleEtSansEmail() {
        when(entrepriseService.existe(7)).thenReturn(true);
        ContactForm form = ContactForm.builder().email(null).formuleDePolitesse(null).nom("Durand").build();

        contactService.saveContact(form, 7);

        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void saveContact_leveUneException_siEmailDejaUtilisePourLEntreprise() {
        when(entrepriseService.existe(7)).thenReturn(true);
        when(contactRepository.existsByEntrepriseIdAndEmailIgnoreCase(7, "doublon@acme.fr")).thenReturn(true);
        ContactForm form = ContactForm.builder().email("doublon@acme.fr").formuleDePolitesse(0).nom("Durand").build();

        assertThatThrownBy(() -> contactService.saveContact(form, 7))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe déjà");

        verify(contactRepository, never()).save(any());
    }

    @Test
    void saveContact_enregistre_siValide() {
        when(entrepriseService.existe(7)).thenReturn(true);
        ContactForm form = ContactForm.builder()
                .email("contact@acme.fr").formuleDePolitesse(0).nom("Durand").build();

        contactService.saveContact(form, 7);

        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void getContactsByIds_renvoieUneMapIndexeeParId() {
        Contact c1 = Contact.builder().id(1L).entrepriseId(7).email("a@b.fr").build();
        Contact c2 = Contact.builder().id(2L).entrepriseId(7).email("c@d.fr").build();
        when(contactRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(c1, c2));

        Map<Long, Contact> result = contactService.getContactsByIds(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(1L).getEmail()).isEqualTo("a@b.fr");
    }

    @Test
    void getContactsByIds_renvoieMapVide_siAucunId() {
        assertThat(contactService.getContactsByIds(List.of())).isEmpty();
        verify(contactRepository, never()).findAllById(any());
    }
}
