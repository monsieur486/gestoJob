package com.mr486.gestojob.tools;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailTools {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailNotificationSender;

    /**
     * Envoie un email HTML. En cas d'échec, lève une {@link MailException}
     * afin que l'appelant ne marque pas l'annonce comme envoyée à tort.
     */
    public void sendHtmlMail(String emailDestinataire, String libelle, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(mailNotificationSender, ApplicationConfiguration.DEFAULT_EMAIL_FROM);                 // expéditeur

            helper.setTo(emailDestinataire);
            helper.setSubject(libelle);
            helper.setText(message, true);

            mailSender.send(mimeMessage);
        } catch (MailException e) {
            log.error("Erreur lors de l'envoi de l'email à {} : {}", emailDestinataire, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la préparation de l'email à {} : {}", emailDestinataire, e.getMessage(), e);
            throw new MailSendException("Échec de l'envoi de l'email à " + emailDestinataire, e);
        }
    }

}
