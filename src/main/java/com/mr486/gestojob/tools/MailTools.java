package com.mr486.gestojob.tools;

import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.service.ProfilService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Service utilitaire d'envoi de courriels.
 *
 * <p>Choisit l'expéditeur du titulaire parmi les {@link JavaMailSenderImpl}
 * configurés (un par boîte Gmail) pour composer et expédier des messages au format
 * HTML ; le nom affiché vient du profil du titulaire.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailTools implements Mailer {

    private final Map<Titulaire, JavaMailSenderImpl> expediteursMail;
    private final ProfilService profilService;

    /**
     * Envoie un email HTML. En cas d'échec, lève une {@link MailException}
     * afin que l'appelant ne marque pas l'annonce comme envoyée à tort.
     *
     * <p><b>Exemple :</b> en cas d'échec SMTP, lève une MailException (l'appelant ne marque pas l'annonce envoyée) ;
     * si Renée n'a pas d'adresse configurée, lève une MetierException.</p>
     *
     * @param titulaire         titulaire dont l'adresse sert d'expéditeur
     * @param emailDestinataire adresse email du destinataire
     * @param libelle           objet (sujet) de l'email
     * @param message           corps de l'email au format HTML
     * @throws MailException   si l'envoi échoue
     * @throws MetierException si l'adresse d'envoi du titulaire n'est pas configurée
     */
    @Override
    public void sendHtmlMail(Titulaire titulaire, String emailDestinataire, String libelle, String message) {
        JavaMailSenderImpl mailSender = expediteursMail.get(titulaire);
        if (mailSender == null) {
            throw new MetierException("Adresse d'envoi non configurée pour " + titulaire.getAffichage());
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // expéditeur
            helper.setFrom(mailSender.getUsername(), profilService.getProfil(titulaire).getCandidatNom());

            helper.setTo(emailDestinataire);
            helper.setSubject(libelle);
            helper.setText(message, true);

            mailSender.send(mimeMessage);
        } catch (MailException e) {
            // L'adresse destinataire n'est pas journalisée (donnée personnelle).
            log.error("erreur lors de l'envoi d'un email : {}", e.getMessage(), e);
            throw e;
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("erreur lors de la préparation d'un email : {}", e.getMessage(), e);
            throw new MailSendException("Échec de l'envoi de l'email", e);
        }
    }

}
