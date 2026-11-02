package com.mr486.gestojob.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr486.gestojob.model.Titulaire;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;

class MailConfigurationTest {

    private final MailConfiguration configuration = new MailConfiguration();

    private MailProperties proprietes() {
        MailProperties p = new MailProperties();
        p.setHost("smtp.exemple.fr");
        p.setPort(587);
        p.getProperties().put("mail.smtp.auth", "true");
        return p;
    }

    private MockEnvironment env(String lu, String lp, String ru, String rp) {
        MockEnvironment e = new MockEnvironment()
                .withProperty("gestojob.mail.laurent.username", lu)
                .withProperty("gestojob.mail.laurent.password", lp);
        if (ru != null) {
            e.withProperty("gestojob.mail.renee.username", ru).withProperty("gestojob.mail.renee.password", rp);
        }
        return e;
    }

    @Test
    void expediteursMail_ignoreLeTitulaireSansUsername() {
        Map<Titulaire, JavaMailSenderImpl> map =
                configuration.expediteursMail(proprietes(), env("laurent@exemple.fr", "mdp", "", ""));

        assertThat(map).containsOnlyKeys(Titulaire.LAURENT);
        JavaMailSenderImpl laurent = map.get(Titulaire.LAURENT);
        assertThat(laurent.getUsername()).isEqualTo("laurent@exemple.fr");
        assertThat(laurent.getPassword()).isEqualTo("mdp");
        assertThat(laurent.getHost()).isEqualTo("smtp.exemple.fr");
        assertThat(laurent.getPort()).isEqualTo(587);
        assertThat(laurent.getJavaMailProperties()).containsEntry("mail.smtp.auth", "true");
    }

    @Test
    void expediteursMail_contientLesDeuxTitulaires_siRenneeConfiguree() {
        Map<Titulaire, JavaMailSenderImpl> map =
                configuration.expediteursMail(proprietes(), env("l@exemple.fr", "a", "r@exemple.fr", "b"));

        assertThat(map).containsOnlyKeys(Titulaire.LAURENT, Titulaire.RENEE);
        assertThat(map.get(Titulaire.RENEE).getUsername()).isEqualTo("r@exemple.fr");
    }

    @Test
    void expediteursMail_utiliseLHoteEtLePortPropresAuTitulaire() {
        MockEnvironment e = env("l@exemple.fr", "a", "r@yahoo.fr", "b")
                .withProperty("gestojob.mail.renee.host", "smtp.mail.yahoo.com")
                .withProperty("gestojob.mail.renee.port", "465");

        Map<Titulaire, JavaMailSenderImpl> map = configuration.expediteursMail(proprietes(), e);

        JavaMailSenderImpl renee = map.get(Titulaire.RENEE);
        assertThat(renee.getHost()).isEqualTo("smtp.mail.yahoo.com");
        assertThat(renee.getPort()).isEqualTo(465);
        assertThat(renee.getJavaMailProperties()).containsEntry("mail.smtp.auth", "true");
        // Laurent, sans hôte propre, garde l'hôte commun.
        assertThat(map.get(Titulaire.LAURENT).getHost()).isEqualTo("smtp.exemple.fr");
    }

    @Test
    void expediteursMail_ignoreUnHoteVide() {
        MockEnvironment e = env("l@exemple.fr", "a", null, null)
                .withProperty("gestojob.mail.laurent.host", " ");

        Map<Titulaire, JavaMailSenderImpl> map = configuration.expediteursMail(proprietes(), e);

        assertThat(map.get(Titulaire.LAURENT).getHost()).isEqualTo("smtp.exemple.fr");
    }

    @Test
    void expediteursMail_ignoreUsernameNul() {
        MailProperties p = new MailProperties();
        p.setHost("h");
        Map<Titulaire, JavaMailSenderImpl> map = configuration.expediteursMail(p, env("l@exemple.fr", "a", null, null));

        assertThat(map).containsOnlyKeys(Titulaire.LAURENT);
    }
}
