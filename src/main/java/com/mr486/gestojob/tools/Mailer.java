package com.mr486.gestojob.tools;

import com.mr486.gestojob.model.Titulaire;
import org.springframework.mail.MailException;

/**
 * Abstraction d'envoi de courriels HTML.
 *
 * <p>Permet aux services métier de dépendre d'un contrat plutôt que d'une
 * implémentation concrète (inversion des dépendances) : l'implémentation de
 * production est {@link MailTools} (SMTP, une boîte Gmail par titulaire), tandis que
 * les tests fournissent un double.
 */
public interface Mailer {

    /**
     * Indique si une adresse d'envoi est configurée pour ce titulaire.
     *
     * <p><b>Exemple :</b> estConfigure(Titulaire.RENEE) retourne false tant que MAIL_RENEE_USERNAME est vide.</p>
     *
     * @param titulaire titulaire concerné
     * @return {@code true} si l'envoi est possible pour ce titulaire
     */
    boolean estConfigure(Titulaire titulaire);

    /**
     * Envoie un email au format HTML. En cas d'échec, lève une {@link MailException}
     * afin que l'appelant ne considère pas l'envoi comme réussi.
     *
     * <p><b>Exemple :</b> sendHtmlMail(Titulaire.RENEE, "dest@exemple.fr", "Objet", "&lt;p&gt;corps&lt;/p&gt;")
     * expédie l'email depuis l'adresse Gmail de Renée ; en cas d'échec SMTP, lève une MailException.</p>
     *
     * @param titulaire         titulaire dont l'adresse sert d'expéditeur
     * @param emailDestinataire adresse email du destinataire
     * @param libelle           objet (sujet) de l'email
     * @param message           corps de l'email au format HTML
     * @throws MailException si l'envoi échoue
     * @throws com.mr486.gestojob.exception.MetierException si l'adresse d'envoi du titulaire n'est pas configurée
     */
    void sendHtmlMail(Titulaire titulaire, String emailDestinataire, String libelle, String message);
}
