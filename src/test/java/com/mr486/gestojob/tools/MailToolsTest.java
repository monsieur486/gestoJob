package com.mr486.gestojob.tools;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailToolsTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailTools mailTools;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailTools, "mailNotificationSender", "expediteur@exemple.fr");
    }

    @Test
    void sendHtmlMail_envoieLeMessage_siOk() {
        MimeMessage mime = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        mailTools.sendHtmlMail("dest@exemple.fr", "Sujet", "<p>corps</p>");

        verify(mailSender).send(mime);
    }

    @Test
    void sendHtmlMail_propageMailException_siEchecEnvoi() {
        MimeMessage mime = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> mailTools.sendHtmlMail("dest@exemple.fr", "Sujet", "<p>corps</p>"))
                .isInstanceOf(MailException.class);
    }
}
