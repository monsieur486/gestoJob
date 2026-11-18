package com.mr486.gestojob.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Le filtre est la seule porte de l'API : un refus doit produire exactement une ligne fail2ban
 * (le contrat de {@link EcouteurEchecConnexion}, avec l'identifiant {@code api}), et jamais le
 * jeton présenté.
 */
// 203.0.113.0/24 est la plage réservée à la documentation (RFC 5737) : un test ne doit
// désigner aucune machine réelle, PMD signale donc ici l'adresse en dur à tort.
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class JetonApiFiltreTest {

    private static final String LOGGER = "securite.connexion";
    private static final String JETON = "jeton-de-test-0123456789";
    private static final String ADRESSE = "203.0.113.7";
    private static final String LIGNE_ATTENDUE = "ECHEC_CONNEXION identifiant=\"api\" adresse=203.0.113.7";

    private ListAppender<ILoggingEvent> lignes;
    private Logger logger;
    private FilterChain chaine;
    private MockHttpServletResponse reponse;

    @BeforeEach
    void brancherLeCollecteur() {
        lignes = new ListAppender<>();
        lignes.start();
        logger = (Logger) LoggerFactory.getLogger(LOGGER);
        logger.addAppender(lignes);
        chaine = mock(FilterChain.class);
        reponse = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void debrancher() {
        logger.detachAppender(lignes);
        SecurityContextHolder.clearContext();
    }

    // Requête venant de l'adresse de documentation, avec l'en-tête du jeton s'il est fourni.
    private MockHttpServletRequest requete(String jeton) {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/api/modele-lettre");
        requete.setRemoteAddr(ADRESSE);
        if (jeton != null) {
            requete.addHeader(JetonApiFiltre.EN_TETE, jeton);
        }
        return requete;
    }

    private Set<String> messages() {
        return lignes.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toSet());
    }

    @Test
    void jetonValide_authentifieLaurentTitulaire() throws Exception {
        new JetonApiFiltre(JETON).doFilter(requete(JETON), reponse, chaine);

        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentification).isNotNull();
        assertThat(authentification.isAuthenticated()).isTrue();
        assertThat(authentification.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ROLE_TITULAIRE", "DONNEES_LAURENT");
        verify(chaine).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        assertThat(lignes.list).isEmpty();
    }

    @Test
    void jetonFaux_401_etLigneFail2ban() throws Exception {
        new JetonApiFiltre(JETON).doFilter(requete("mauvais-jeton"), reponse, chaine);

        assertThat(reponse.getStatus()).isEqualTo(401);
        assertThat(lignes.list).hasSize(1);
        assertThat(lignes.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(lignes.list.get(0).getFormattedMessage()).isEqualTo(LIGNE_ATTENDUE);
        verify(chaine, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void enTeteAbsent_401_etLigneFail2ban() throws Exception {
        new JetonApiFiltre(JETON).doFilter(requete(null), reponse, chaine);

        assertThat(reponse.getStatus()).isEqualTo(401);
        assertThat(lignes.list).hasSize(1);
        assertThat(lignes.list.get(0).getFormattedMessage()).isEqualTo(LIGNE_ATTENDUE);
        verify(chaine, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void jetonConfigureVide_etEnTeteVide_401() throws Exception {
        // Une API non configurée (jeton vide) ne doit jamais s'ouvrir à un en-tête vide,
        // que MessageDigest.isEqual jugerait pourtant égal.
        new JetonApiFiltre("").doFilter(requete(""), reponse, chaine);

        assertThat(reponse.getStatus()).isEqualTo(401);
        assertThat(lignes.list).hasSize(1);
        verify(chaine, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void jetonConfigureBlanc_refuseLeMemeBlanc() throws Exception {
        new JetonApiFiltre("   ").doFilter(requete("   "), reponse, chaine);

        assertThat(reponse.getStatus()).isEqualTo(401);
        verify(chaine, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void reponse401_estDuJson() throws Exception {
        new JetonApiFiltre(JETON).doFilter(requete("mauvais-jeton"), reponse, chaine);

        assertThat(reponse.getContentType()).startsWith("application/json");
        assertThat(reponse.getContentAsString()).isEqualTo("{\"erreur\":\"Authentification requise.\"}");
    }

    @Test
    void leJetonNApparaitDansAucuneLigne() throws Exception {
        String envoye = "jeton-secret-envoye-par-un-attaquant";
        new JetonApiFiltre(JETON).doFilter(requete(envoye), reponse, chaine);

        assertThat(messages()).isNotEmpty().noneMatch(ligne -> ligne.contains(envoye) || ligne.contains(JETON));
    }
}
