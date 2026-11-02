package com.mr486.gestojob.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.service.ProfilService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailToolsTest {

    private final JavaMailSenderImpl senderRenee = mock(JavaMailSenderImpl.class);
    private MailTools mailTools;

    @BeforeEach
    void setUp() {
        when(senderRenee.getUsername()).thenReturn("renee@exemple.fr");
        ProfilService profilService = mock(ProfilService.class);
        Profil profil = new Profil();
        profil.setCandidatNom("Renée");
        when(profilService.getProfil(Titulaire.RENEE)).thenReturn(profil);
        mailTools = new MailTools(Map.of(Titulaire.RENEE, senderRenee), profilService);
    }

    @Test
    void sendHtmlMail_utiliseLExpediteurEtLeNomDuTitulaire() throws Exception {
        MimeMessage mime = new MimeMessage((Session) null);
        when(senderRenee.createMimeMessage()).thenReturn(mime);

        mailTools.sendHtmlMail(Titulaire.RENEE, "dest@exemple.fr", "Sujet", "<p>corps</p>");

        verify(senderRenee).send(mime);
        InternetAddress from = (InternetAddress) mime.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("renee@exemple.fr");
        assertThat(from.getPersonal()).isEqualTo("Renée");
    }

    @Test
    void sendHtmlMail_titulaireNonConfigure_leveMetierException() {
        assertThatThrownBy(() -> mailTools.sendHtmlMail(Titulaire.LAURENT, "d@exemple.fr", "S", "m"))
                .isInstanceOf(MetierException.class)
                .hasMessage("Adresse d'envoi non configurée pour Laurent");

        verify(senderRenee, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendHtmlMail_propageMailException_siEchecEnvoi() {
        MimeMessage mime = new MimeMessage((Session) null);
        when(senderRenee.createMimeMessage()).thenReturn(mime);
        doThrow(new MailSendException("smtp down")).when(senderRenee).send(any(MimeMessage.class));

        assertThatThrownBy(() -> mailTools.sendHtmlMail(Titulaire.RENEE, "dest@exemple.fr", "Sujet", "<p>corps</p>"))
                .isInstanceOf(MailException.class);
    }
}
