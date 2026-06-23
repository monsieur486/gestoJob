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

/**
 * Service utilitaire d'envoi de courriels.
 * <p>
 * S'appuie sur {@link JavaMailSender} pour composer et expédier des messages
 * au format HTML, en utilisant l'adresse d'expéditeur configurée pour
 * l'application.
 */
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
     *
     * <p><b>Exemple :</b> en cas d'échec SMTP, lève une MailException (l'appelant ne marque pas l'annonce envoyée).</p>
     *
     * @param emailDestinataire adresse email du destinataire
     * @param libelle           objet (sujet) de l'email
     * @param message           corps de l'email au format HTML
     * @throws MailException si l'envoi échoue
     */
    public void sendHtmlMail(String emailDestinataire, String libelle, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // expéditeur
            helper.setFrom(mailNotificationSender, ApplicationConfiguration.DEFAULT_EMAIL_FROM);

            helper.setTo(emailDestinataire);
            helper.setSubject(libelle);
            helper.setText(message, true);

            mailSender.send(mimeMessage);
        } catch (MailException e) {
            // L'adresse destinataire n'est pas journalisée (donnée personnelle).
            log.error("Erreur lors de l'envoi d'un email : {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la préparation d'un email : {}", e.getMessage(), e);
            throw new MailSendException("Échec de l'envoi de l'email", e);
        }
    }

}
