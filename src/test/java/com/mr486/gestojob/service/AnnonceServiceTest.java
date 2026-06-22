package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceListe;
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
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
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
class AnnonceServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;
    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private ContactService contactService;
    @Mock
    private MailTools mailTools;
    @Mock
    private ContenuService contenuService;

    @InjectMocks
    private AnnonceService annonceService;

    private Annonce annonce(long id, long contactId, String email) {
        Contact contact = Contact.builder()
                .id(contactId).entrepriseId(10).email(email).formuleDePolistesse(0).build();
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

        annonceService.sendDirectEmail(1L);

        verify(annonceRepository).updateStatusAnnonceEtDateEnvoi(eq(1L), eq(2), any(OffsetDateTime.class));
    }

    @Test
    void sendDirectEmail_neMarquePasEnvoye_siEchecMail() {
        Annonce a = annonce(1L, 5L, "ko@exemple.fr");
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(contenuService.getHtmlContenu(any(), anyInt(), any())).thenReturn("<p>html</p>");
        doThrow(new MailSendException("smtp down")).when(mailTools).sendHtmlMail(any(), any(), any());

        assertThatThrownBy(() -> annonceService.sendDirectEmail(1L))
                .isInstanceOf(MailSendException.class);

        verify(annonceRepository, never()).updateStatusAnnonceEtDateEnvoi(any(), any(), any());
    }

    @Test
    void sendEmailForPendingAnnonces_continueApresEchec_etNeMarqueQueLesSucces() {
        Annonce ok = annonce(1L, 5L, "ok@exemple.fr");
        Annonce ko = annonce(2L, 6L, "ko@exemple.fr");
        when(annonceRepository.findAllByStatusAnnonce(1)).thenReturn(List.of(ok, ko));
        when(contenuService.getHtmlContenu(any(), anyInt(), any())).thenReturn("<p>html</p>");
        doNothing().when(mailTools).sendHtmlMail(eq("ok@exemple.fr"), any(), any());
        doThrow(new MailSendException("smtp down"))
                .when(mailTools).sendHtmlMail(eq("ko@exemple.fr"), any(), any());

        annonceService.sendEmailForPendingAnnonces();

        verify(annonceRepository).updateStatusAnnonceEtDateEnvoi(eq(1L), eq(2), any(OffsetDateTime.class));
        verify(annonceRepository, never())
                .updateStatusAnnonceEtDateEnvoi(eq(2L), any(), any());
    }

    @Test
    void annoncesListeByEntrepriseId_triParDateDecroissante_nullsEnDernier() {
        Annonce janvier = Annonce.builder()
                .id(1L).entrepriseId(10).typeAnnonce(0).typeContenu(0).statusAnnonce(2)
                .dateEnvoi(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)).build();
        Annonce juin = Annonce.builder()
                .id(2L).entrepriseId(10).typeAnnonce(0).typeContenu(0).statusAnnonce(2)
                .dateEnvoi(OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC)).build();
        Annonce sansDate = Annonce.builder()
                .id(3L).entrepriseId(10).typeAnnonce(0).typeContenu(0).statusAnnonce(1)
                .dateEnvoi(null).build();

        // volontairement non triées en entrée
        when(annonceRepository.findAllByEntrepriseIdOrderByStatusAnnonceAsc(10))
                .thenReturn(new java.util.ArrayList<>(List.of(janvier, sansDate, juin)));
        when(entrepriseService.getEntreprisesByIds(any())).thenReturn(Collections.emptyMap());
        when(contactService.getContactsByIds(any())).thenReturn(Collections.emptyMap());

        List<AnnonceListe> result = annonceService.annoncesListeByEntrepriseId(10);

        // juin (plus récent) -> janvier -> sans date (en dernier)
        assertThat(result).extracting(AnnonceListe::getId).containsExactly(2L, 1L, 3L);
    }
}
