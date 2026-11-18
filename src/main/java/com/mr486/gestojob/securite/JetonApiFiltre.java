package com.mr486.gestojob.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentifie les appels de l'API Claude ({@code /api/**}) par un jeton partagé, porté par
 * l'en-tête {@value #EN_TETE}.
 *
 * <p>Un jeton valide ouvre une session sans état au nom du titulaire {@code LAURENT}, avec le
 * rôle {@code TITULAIRE}. Tout autre cas (en-tête absent ou faux, jeton attendu non configuré)
 * reçoit un {@code 401} JSON et produit <b>une seule</b> ligne sur le journal
 * {@code securite.connexion}, au format lu par fail2ban (voir {@link EcouteurEchecConnexion}).
 * Ni le jeton attendu ni la valeur reçue ne sont jamais journalisés.</p>
 *
 * <p>Ce filtre n'est volontairement <b>pas</b> un composant Spring : Spring Boot l'enregistrerait
 * alors comme filtre servlet global, actif aussi sur les pages. Il n'est instancié que par
 * {@code ApiSecurityConfig}, dans la chaîne réservée à {@code /api/**}.</p>
 *
 * <p><b>Exemple :</b> {@code curl -H "X-GestoJob-Jeton: <jeton>" https://…/api/modele-lettre}.</p>
 */
public class JetonApiFiltre extends OncePerRequestFilter {

    /** Nom de l'en-tête HTTP qui porte le jeton de l'API. */
    public static final String EN_TETE = "X-GestoJob-Jeton";

    /** Corps de la réponse de refus, fixe : il ne dit rien de la cause du refus. */
    private static final String CORPS_401 = "{\"erreur\":\"Authentification requise.\"}";

    /** Identifiant porté par la ligne fail2ban et par l'authentification accordée. */
    private static final String IDENTIFIANT = "api";

    /** Journal dédié lu par fail2ban (même nom que celui de {@link EcouteurEchecConnexion}). */
    private static final Logger JOURNAL = LoggerFactory.getLogger("securite.connexion");

    private final String jetonAttendu;

    /**
     * Crée le filtre avec le jeton attendu, lu dans la configuration.
     *
     * <p><b>Exemple :</b> {@code new JetonApiFiltre("…48 octets aléatoires en base64…")} ;
     * {@code new JetonApiFiltre("")} refuse tout appel (API désactivée).</p>
     *
     * @param jetonAttendu le jeton configuré ; vide, blanc ou {@code null}, l'API est fermée
     */
    public JetonApiFiltre(String jetonAttendu) {
        this.jetonAttendu = jetonAttendu;
    }

    /**
     * Laisse passer la requête si l'en-tête porte le jeton attendu, la refuse sinon.
     *
     * <p><b>Exemple :</b> un appel sans en-tête depuis {@code 203.0.113.7} reçoit {@code 401} et
     * produit la ligne {@code ECHEC_CONNEXION identifiant="api" adresse=203.0.113.7}.</p>
     *
     * @param requete la requête HTTP
     * @param reponse la réponse HTTP
     * @param chaine  la suite de la chaîne de filtres, appelée seulement si le jeton est valide
     * @throws ServletException si la suite de la chaîne échoue
     * @throws IOException      si l'écriture de la réponse échoue
     */
    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        if (!jetonValide(requete.getHeader(EN_TETE))) {
            refuser(requete, reponse);
            return;
        }
        SecurityContext contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(IDENTIFIANT, null,
                List.of(new SimpleGrantedAuthority("ROLE_TITULAIRE"),
                        new SimpleGrantedAuthority("DONNEES_LAURENT"))));
        SecurityContextHolder.setContext(contexte);
        chaine.doFilter(requete, reponse);
    }

    // Comparaison en temps constant, et seulement si un jeton est configuré : sans ce garde,
    // une API non configurée (jeton vide) s'ouvrirait à un en-tête vide.
    private boolean jetonValide(String recu) {
        if (jetonAttendu == null || jetonAttendu.isBlank() || recu == null) {
            return false;
        }
        return MessageDigest.isEqual(jetonAttendu.getBytes(StandardCharsets.UTF_8),
                recu.getBytes(StandardCharsets.UTF_8));
    }

    // Écrit la ligne fail2ban (jamais le jeton ni l'en-tête) puis le 401 JSON.
    private void refuser(HttpServletRequest requete, HttpServletResponse reponse) throws IOException {
        JOURNAL.warn("ECHEC_CONNEXION identifiant=\"{}\" adresse={}", IDENTIFIANT, requete.getRemoteAddr());
        reponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        reponse.setContentType("application/json;charset=UTF-8");
        reponse.getWriter().write(CORPS_401);
    }
}
