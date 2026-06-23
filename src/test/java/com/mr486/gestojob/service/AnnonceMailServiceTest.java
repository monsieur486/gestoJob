package com.mr486.gestojob.service;

import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.tools.MailTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnonceMailServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;
    @Mock
    private ContactService contactService;
    @Mock
    private ContenuService contenuService;
    @Mock
    private MailTools mailTools;

    @InjectMocks
    private AnnonceMailService annonceMailService;

    private Annonce annonce(long id, long contactId, String email) {
        Contact contact = Contact.builder()
                .id(contactId).entrepriseId(10).email(email).formuleDePolitesse(0).build();
        when(contactService.getContact(contactId)).thenReturn(contact);
        return Annonce.builder()
                .id(id).entrepriseId(10).contactId(contactId)
                .typeAnnonce(0).typeContenu(0).poste("Dev").reference("R")
                .statusAnnonce(1).build();
    }

    @Test
    void sendDirectEmail_marqueEnvoye_siSucces() {
        Annonce a = annonce(1L, 5L, "ok@exemple.fr");
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(contenuService.getHtmlContenu(any(), anyInt(), any())).thenReturn("<p>html</p>");
        doNothing().when(mailTools).sendHtmlMail(any(), any(), any());

        annonceMailService.sendDirectEmail(1L);

        verify(annonceRepository).updateStatusAnnonceEtDateEnvoi(eq(1L), eq(2), any(OffsetDateTime.class));
    }

    @Test
    void sendDirectEmail_neMarquePasEnvoye_siEchecMail() {
        Annonce a = annonce(1L, 5L, "ko@exemple.fr");
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(contenuService.getHtmlContenu(any(), anyInt(), any())).thenReturn("<p>html</p>");
        doThrow(new MailSendException("smtp down")).when(mailTools).sendHtmlMail(any(), any(), any());

        assertThatThrownBy(() -> annonceMailService.sendDirectEmail(1L))
                .isInstanceOf(MailSendException.class);

        verify(annonceRepository, never()).updateStatusAnnonceEtDateEnvoi(any(), any(), any());
    }

    @Test
    void sendDirectEmail_leveUneException_siAnnonceIntrouvable() {
        when(annonceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> annonceMailService.sendDirectEmail(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void sendDirectEmail_leveUneException_siAnnonceSansContact() {
        Annonce sansContact = Annonce.builder()
                .id(1L).entrepriseId(10).contactId(null)
                .typeAnnonce(0).typeContenu(0).poste("Dev").statusAnnonce(1).build();
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(sansContact));

        assertThatThrownBy(() -> annonceMailService.sendDirectEmail(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contact");

        verify(mailTools, never()).sendHtmlMail(any(), any(), any());
        verify(annonceRepository, never()).updateStatusAnnonceEtDateEnvoi(any(), any(), any());
    }

    @Test
    void sendEmailForPendingAnnonces_continueApresEchec_etNeMarqueQueLesSucces() {
        Annonce ok = Annonce.builder().id(1L).entrepriseId(10).contactId(5L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").reference("R").statusAnnonce(1).build();
        Annonce ko = Annonce.builder().id(2L).entrepriseId(10).contactId(6L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").reference("R").statusAnnonce(1).build();
        when(annonceRepository.findAllByStatusAnnonce(1)).thenReturn(List.of(ok, ko));
        when(contactService.getContactsByIds(any())).thenReturn(Map.of(
                5L, Contact.builder().id(5L).entrepriseId(10).email("ok@exemple.fr").formuleDePolitesse(0).build(),
                6L, Contact.builder().id(6L).entrepriseId(10).email("ko@exemple.fr").formuleDePolitesse(0).build()));
        when(contenuService.getHtmlContenu(any(), anyInt(), any())).thenReturn("<p>html</p>");
        doNothing().when(mailTools).sendHtmlMail(eq("ok@exemple.fr"), any(), any());
        doThrow(new MailSendException("smtp down"))
                .when(mailTools).sendHtmlMail(eq("ko@exemple.fr"), any(), any());

        annonceMailService.sendEmailForPendingAnnonces();

        verify(annonceRepository).updateStatusAnnonceEtDateEnvoi(eq(1L), eq(2), any(OffsetDateTime.class));
        verify(annonceRepository, never())
                .updateStatusAnnonceEtDateEnvoi(eq(2L), any(), any());
    }

    @Test
    void sendEmailForPendingAnnonces_chargeLesContactsEnLot_sansN1() {
        Annonce a1 = Annonce.builder().id(1L).entrepriseId(10).contactId(5L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").statusAnnonce(1).build();
        Annonce a2 = Annonce.builder().id(2L).entrepriseId(10).contactId(6L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").statusAnnonce(1).build();
        when(annonceRepository.findAllByStatusAnnonce(1)).thenReturn(List.of(a1, a2));
        when(contactService.getContactsByIds(any())).thenReturn(Map.of(
                5L, Contact.builder().id(5L).entrepriseId(10).email("a@x.fr").formuleDePolitesse(0).build(),
                6L, Contact.builder().id(6L).entrepriseId(10).email("b@x.fr").formuleDePolitesse(0).build()));
        when(contenuService.getHtmlContenu(any(), anyInt(), any())).thenReturn("<p>html</p>");
        doNothing().when(mailTools).sendHtmlMail(any(), any(), any());

        annonceMailService.sendEmailForPendingAnnonces();

        verify(contactService).getContactsByIds(any());
        verify(contactService, never()).getContact(any());
        verify(mailTools).sendHtmlMail(eq("a@x.fr"), any(), any());
        verify(mailTools).sendHtmlMail(eq("b@x.fr"), any(), any());
    }

    @Test
    void getHtmlContent_utiliseFormuleGenerique_siPasDeContact() {
        Annonce a = Annonce.builder().id(1L).contactId(null).poste("Dev").typeContenu(0).build();
        when(contenuService.getHtmlContenu("Dev", 0, "Madame, Monsieur,")).thenReturn("HTML");

        assertThat(annonceMailService.getHtmlContent(a)).isEqualTo("HTML");
    }
}
