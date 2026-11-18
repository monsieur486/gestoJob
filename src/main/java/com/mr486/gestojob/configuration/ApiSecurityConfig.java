package com.mr486.gestojob.configuration;

import com.mr486.gestojob.securite.JetonApiFiltre;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Chaîne de sécurité de l'API Claude ({@code /api/**}), distincte de celle des pages.
 *
 * <p>Placée avant la chaîne des pages ({@link SecurityConfig}, qui reste le cas général), elle ne
 * reconnaît <b>que</b> le jeton de l'en-tête {@value JetonApiFiltre#EN_TETE} : une session ouverte
 * par le formulaire de connexion n'y vaut rien. Sans état, elle n'a ni session, ni CSRF (aucun
 * cookie à détourner), ni formulaire de connexion, ni déconnexion. Les refus sont rendus en
 * JSON.</p>
 *
 * <p><b>Exemple :</b> {@code GET /api/modele-lettre} sans en-tête reçoit {@code 401}
 * {@code {"erreur":"Authentification requise."}} ; {@code GET /entreprises} reste servi par la
 * chaîne des pages.</p>
 */
@Configuration
public class ApiSecurityConfig {

    /** Corps du refus faute d'authentification. */
    private static final String CORPS_401 = "{\"erreur\":\"Authentification requise.\"}";

    /** Corps du refus faute de droits. */
    private static final String CORPS_403 = "{\"erreur\":\"Accès refusé.\"}";

    /**
     * Construit la chaîne de filtres de {@code /api/**}, authentifiée par jeton.
     *
     * <p><b>Exemple :</b> avec {@code GESTOJOB_API_TOKEN} vide, tout appel à {@code /api/**}
     * reçoit {@code 401} : l'API est désactivée.</p>
     *
     * @param http  le constructeur de configuration HTTP
     * @param jeton le jeton attendu ({@code gestojob.api.token}) ; vide, l'API est fermée
     * @return la chaîne de filtres de l'API
     * @throws Exception si la configuration échoue
     */
    // « throws Exception » est imposé par l'API Spring Security (HttpSecurity#build).
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, @Value("${gestojob.api.token:}") String jeton)
            throws Exception {
        http
                .securityMatcher("/api/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("TITULAIRE"))
                // Instancié ici seulement : déclaré en bean, il filtrerait aussi les pages.
                .addFilterBefore(new JetonApiFiltre(jeton), AnonymousAuthenticationFilter.class)
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint((requete, reponse, exception) ->
                                repondre(reponse, HttpServletResponse.SC_UNAUTHORIZED, CORPS_401))
                        .accessDeniedHandler((requete, reponse, exception) ->
                                repondre(reponse, HttpServletResponse.SC_FORBIDDEN, CORPS_403)));
        return http.build();
    }

    // Écrit un refus JSON au corps fixe. Pas de ligne fail2ban ici : le filtre l'a déjà écrite
    // pour tout jeton refusé, et en écrire une seconde fausserait le décompte des échecs.
    private static void repondre(HttpServletResponse reponse, int statut, String corps) throws IOException {
        reponse.setStatus(statut);
        reponse.setContentType("application/json;charset=UTF-8");
        reponse.getWriter().write(corps);
    }
}
