package com.mr486.gestojob.service;

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

import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import com.mr486.gestojob.tools.Mailer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;

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
    private Mailer mailer;
    @Mock
    private LibelleService libelleService;

    @Mock
    private UtilisateurCourant utilisateurCourant;

    @InjectMocks
    private AnnonceMailService annonceMailService;

    @BeforeEach
    void setUp() {
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
    }

    private Annonce annonce(long id, long contactId, String email) {
        Contact contact = Contact.builder()
                .id(contactId).entrepriseId(10).email(email).formuleDePolitesse(0).build();
        when(contactService.getContact(contactId)).thenReturn(contact);
        return Annonce.builder()
                .id(id).entrepriseId(10).contactId(contactId)
                .typeAnnonce(0).typeContenu(0).poste("Dev").reference("R")
                .statusAnnonce(1)
                .titulaire(Titulaire.RENEE).build();
    }

    @Test
    void sendDirectEmail_marqueEnvoye_siSucces() {
        Annonce a = annonce(1L, 5L, "ok@exemple.fr");
        when(annonceRepository.findByIdAndTitulaire(1L, Titulaire.RENEE)).thenReturn(Optional.of(a));
        when(contenuService.getHtmlContenu(any(), any(), anyInt(), any())).thenReturn("<p>html</p>");
        when(libelleService.construitLibelle(org.mockito.ArgumentMatchers.any())).thenReturn("objet de test");
        doNothing().when(mailer).sendHtmlMail(any(), any(), any(), any());

        annonceMailService.sendDirectEmail(1L);

        verify(annonceRepository).updateStatusAnnonceEtDateEnvoi(
                eq(Titulaire.RENEE), eq(1L), eq(2), any(OffsetDateTime.class));
    }

    @Test
    void sendDirectEmail_titulaireNonConfigure_statutInchange() {
        Annonce a = annonce(1L, 5L, "ok@exemple.fr");
        when(annonceRepository.findByIdAndTitulaire(1L, Titulaire.RENEE)).thenReturn(Optional.of(a));
        when(contenuService.getHtmlContenu(any(), any(), anyInt(), any())).thenReturn("<p>html</p>");
        when(libelleService.construitLibelle(any())).thenReturn("objet de test");
        doThrow(new MetierException("Adresse d'envoi non configurée pour Renée"))
                .when(mailer).sendHtmlMail(eq(Titulaire.RENEE), any(), any(), any());

        assertThatThrownBy(() -> annonceMailService.sendDirectEmail(1L))
                .isInstanceOf(MetierException.class);

        verify(annonceRepository, never()).updateStatusAnnonceEtDateEnvoi(any(), any(), any(), any());
    }

    @Test
    void sendEmailForPendingAnnonces_nEnvoieQueLesAnnoncesDuTitulaireCourant() {
        Annonce a = Annonce.builder().id(1L).entrepriseId(10).contactId(5L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").statusAnnonce(1)
                .titulaire(Titulaire.RENEE).build();
        when(annonceRepository.findAllByTitulaireAndStatusAnnonce(Titulaire.RENEE, 1)).thenReturn(List.of(a));
        when(contactService.getContactsByIds(any())).thenReturn(Map.of(
                5L, Contact.builder().id(5L).entrepriseId(10).email("a@x.fr").formuleDePolitesse(0).build()));
        when(contenuService.getHtmlContenu(any(), any(), anyInt(), any())).thenReturn("<p>html</p>");
        when(libelleService.construitLibelle(any())).thenReturn("objet");

        annonceMailService.sendEmailForPendingAnnonces();

        verify(annonceRepository).findAllByTitulaireAndStatusAnnonce(Titulaire.RENEE, 1);
        verify(annonceRepository, never()).findAllByTitulaireAndStatusAnnonce(Titulaire.LAURENT, 1);
        verify(mailer).sendHtmlMail(eq(Titulaire.RENEE), eq("a@x.fr"), any(), any());
    }

    @Test
    void sendDirectEmail_neMarquePasEnvoye_siEchecMail() {
        Annonce a = annonce(1L, 5L, "ko@exemple.fr");
        when(annonceRepository.findByIdAndTitulaire(1L, Titulaire.RENEE)).thenReturn(Optional.of(a));
        when(contenuService.getHtmlContenu(any(), any(), anyInt(), any())).thenReturn("<p>html</p>");
        when(libelleService.construitLibelle(org.mockito.ArgumentMatchers.any())).thenReturn("objet de test");
        doThrow(new MailSendException("smtp down")).when(mailer).sendHtmlMail(any(), any(), any(), any());

        assertThatThrownBy(() -> annonceMailService.sendDirectEmail(1L))
                .isInstanceOf(MailSendException.class);

        verify(annonceRepository, never()).updateStatusAnnonceEtDateEnvoi(any(), any(), any(), any());
    }

    @Test
    void sendDirectEmail_leveUneException_siAnnonceIntrouvable() {
        when(annonceRepository.findByIdAndTitulaire(404L, Titulaire.RENEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> annonceMailService.sendDirectEmail(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void sendDirectEmail_leveUneException_siAnnonceSansContact() {
        Annonce sansContact = Annonce.builder()
                .id(1L).entrepriseId(10).contactId(null)
                .typeAnnonce(0).typeContenu(0).poste("Dev").statusAnnonce(1)
                .titulaire(Titulaire.RENEE).build();
        when(annonceRepository.findByIdAndTitulaire(1L, Titulaire.RENEE)).thenReturn(Optional.of(sansContact));

        assertThatThrownBy(() -> annonceMailService.sendDirectEmail(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contact");

        verify(mailer, never()).sendHtmlMail(any(), any(), any(), any());
        verify(annonceRepository, never()).updateStatusAnnonceEtDateEnvoi(any(), any(), any(), any());
    }

    @Test
    void sendEmailForPendingAnnonces_continueApresEchec_etNeMarqueQueLesSucces() {
        Annonce ok = Annonce.builder().id(1L).entrepriseId(10).contactId(5L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").reference("R").statusAnnonce(1)
                .titulaire(Titulaire.RENEE).build();
        Annonce ko = Annonce.builder().id(2L).entrepriseId(10).contactId(6L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").reference("R").statusAnnonce(1)
                .titulaire(Titulaire.RENEE).build();
        when(annonceRepository.findAllByTitulaireAndStatusAnnonce(Titulaire.RENEE, 1)).thenReturn(List.of(ok, ko));
        when(contactService.getContactsByIds(any())).thenReturn(Map.of(
                5L, Contact.builder().id(5L).entrepriseId(10).email("ok@exemple.fr").formuleDePolitesse(0).build(),
                6L, Contact.builder().id(6L).entrepriseId(10).email("ko@exemple.fr").formuleDePolitesse(0).build()));
        when(contenuService.getHtmlContenu(any(), any(), anyInt(), any())).thenReturn("<p>html</p>");
        when(libelleService.construitLibelle(org.mockito.ArgumentMatchers.any())).thenReturn("objet de test");
        doNothing().when(mailer).sendHtmlMail(any(), eq("ok@exemple.fr"), any(), any());
        doThrow(new MailSendException("smtp down"))
                .when(mailer).sendHtmlMail(any(), eq("ko@exemple.fr"), any(), any());

        annonceMailService.sendEmailForPendingAnnonces();

        verify(annonceRepository).updateStatusAnnonceEtDateEnvoi(
                eq(Titulaire.RENEE), eq(1L), eq(2), any(OffsetDateTime.class));
        verify(annonceRepository, never())
                .updateStatusAnnonceEtDateEnvoi(any(), eq(2L), any(), any());
    }

    @Test
    void sendEmailForPendingAnnonces_chargeLesContactsEnLot_sansN1() {
        Annonce a1 = Annonce.builder().id(1L).entrepriseId(10).contactId(5L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").statusAnnonce(1)
                .titulaire(Titulaire.RENEE).build();
        Annonce a2 = Annonce.builder().id(2L).entrepriseId(10).contactId(6L)
                .typeAnnonce(0).typeContenu(0).poste("Dev").statusAnnonce(1)
                .titulaire(Titulaire.RENEE).build();
        when(annonceRepository.findAllByTitulaireAndStatusAnnonce(Titulaire.RENEE, 1)).thenReturn(List.of(a1, a2));
        when(contactService.getContactsByIds(any())).thenReturn(Map.of(
                5L, Contact.builder().id(5L).entrepriseId(10).email("a@x.fr").formuleDePolitesse(0).build(),
                6L, Contact.builder().id(6L).entrepriseId(10).email("b@x.fr").formuleDePolitesse(0).build()));
        when(contenuService.getHtmlContenu(any(), any(), anyInt(), any())).thenReturn("<p>html</p>");
        when(libelleService.construitLibelle(org.mockito.ArgumentMatchers.any())).thenReturn("objet de test");
        doNothing().when(mailer).sendHtmlMail(any(), any(), any(), any());

        annonceMailService.sendEmailForPendingAnnonces();

        verify(contactService).getContactsByIds(any());
        verify(contactService, never()).getContact(any());
        verify(mailer).sendHtmlMail(eq(Titulaire.RENEE), eq("a@x.fr"), any(), any());
        verify(mailer).sendHtmlMail(eq(Titulaire.RENEE), eq("b@x.fr"), any(), any());
    }

    @Test
    void getHtmlContent_utiliseFormuleGenerique_siPasDeContact() {
        Annonce a = Annonce.builder().id(1L).contactId(null).poste("Dev")
                .titulaire(Titulaire.RENEE).typeContenu(0).build();
        when(contenuService.getHtmlContenu(Titulaire.RENEE, "Dev", 0, "Madame, Monsieur,"))
                .thenReturn("HTML");

        assertThat(annonceMailService.getHtmlContent(a)).isEqualTo("HTML");
    }
}
