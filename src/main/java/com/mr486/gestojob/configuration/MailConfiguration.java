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
 * titulaire, chacun authentifié sur sa propre boîte (Gmail pour Laurent, Yahoo pour Renée).
 *
 * <p>Les propriétés SMTP sont partagées (clés {@code spring.mail.*}). L'hôte et le port
 * communs servent de valeur par défaut ; chaque titulaire peut les remplacer par
 * {@code gestojob.mail.<titulaire>.host|port}. L'identifiant et le mot de passe sont
 * toujours propres au titulaire.
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
     * @param proprietes hôte et port par défaut, propriétés SMTP partagées
     * @param environnement source des clés {@code gestojob.mail.<titulaire>.username|password|host|port}
     * @return les expéditeurs configurés, par titulaire
     */
    @Bean
    public Map<Titulaire, JavaMailSenderImpl> expediteursMail(MailProperties proprietes, Environment environnement) {
        Map<Titulaire, JavaMailSenderImpl> expediteurs = new EnumMap<>(Titulaire.class);
        for (Titulaire titulaire : Titulaire.values()) {
            String prefixe = "gestojob.mail." + titulaire.name().toLowerCase(Locale.ROOT) + ".";
            ajouter(expediteurs, titulaire, proprietes, environnement, prefixe);
        }
        return expediteurs;
    }

    // N'enregistre l'expéditeur que si l'identifiant est renseigné ; l'hôte et le port
    // propres au titulaire priment sur les valeurs communes.
    private void ajouter(Map<Titulaire, JavaMailSenderImpl> expediteurs, Titulaire titulaire,
                         MailProperties proprietes, Environment environnement, String prefixe) {
        String username = environnement.getProperty(prefixe + "username");
        if (username == null || username.isBlank()) {
            return;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        String host = environnement.getProperty(prefixe + "host");
        sender.setHost(host == null || host.isBlank() ? proprietes.getHost() : host);
        Integer port = environnement.getProperty(prefixe + "port", Integer.class, proprietes.getPort());
        if (port != null) {
            sender.setPort(port);
        }
        sender.setDefaultEncoding("UTF-8");
        if (!proprietes.getProperties().isEmpty()) {
            Properties javaMail = new Properties();
            javaMail.putAll(proprietes.getProperties());
            sender.setJavaMailProperties(javaMail);
        }
        sender.setUsername(username);
        sender.setPassword(environnement.getProperty(prefixe + "password"));
        expediteurs.put(titulaire, sender);
    }
}
