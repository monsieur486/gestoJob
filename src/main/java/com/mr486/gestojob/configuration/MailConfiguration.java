package com.mr486.gestojob.configuration;

import com.mr486.gestojob.model.Titulaire;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configuration des expéditeurs de courriel : un {@link JavaMailSenderImpl} par
 * titulaire, chacun authentifié sur sa propre boîte Gmail.
 *
 * <p>L'hôte, le port et les propriétés SMTP sont partagés (clés {@code spring.mail.*}) ;
 * seuls l'identifiant et le mot de passe diffèrent d'un titulaire à l'autre.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfiguration {

    /**
     * Construit les expéditeurs des titulaires dont l'adresse est configurée.
     * Un titulaire sans identifiant (Renée est facultative) est absent de la map.
     *
     * <p><b>Exemple :</b> avec seulement MAIL_LAURENT_USERNAME renseigné, la map
     * ne contient que {@code LAURENT}.</p>
     *
     * @param proprietes hôte, port et propriétés SMTP partagés
     * @param environnement source des clés {@code gestojob.mail.<titulaire>.username|password}
     * @return les expéditeurs configurés, par titulaire
     */
    @Bean
    public Map<Titulaire, JavaMailSenderImpl> expediteursMail(MailProperties proprietes, Environment environnement) {
        Map<Titulaire, JavaMailSenderImpl> expediteurs = new EnumMap<>(Titulaire.class);
        for (Titulaire titulaire : Titulaire.values()) {
            String prefixe = "gestojob.mail." + titulaire.name().toLowerCase(Locale.ROOT) + ".";
            ajouter(expediteurs, titulaire, proprietes,
                    environnement.getProperty(prefixe + "username"), environnement.getProperty(prefixe + "password"));
        }
        return expediteurs;
    }

    // N'enregistre l'expéditeur que si l'identifiant est renseigné.
    private void ajouter(Map<Titulaire, JavaMailSenderImpl> expediteurs, Titulaire titulaire,
                         MailProperties proprietes, String username, String password) {
        if (username == null || username.isBlank()) {
            return;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(proprietes.getHost());
        if (proprietes.getPort() != null) {
            sender.setPort(proprietes.getPort());
        }
        sender.setDefaultEncoding("UTF-8");
        if (!proprietes.getProperties().isEmpty()) {
            Properties javaMail = new Properties();
            javaMail.putAll(proprietes.getProperties());
            sender.setJavaMailProperties(javaMail);
        }
        sender.setUsername(username);
        sender.setPassword(password);
        expediteurs.put(titulaire, sender);
    }
}
