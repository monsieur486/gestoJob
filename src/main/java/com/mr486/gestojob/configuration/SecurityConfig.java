package com.mr486.gestojob.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de la sécurité applicative.
 *
 * <p>Seules la page de connexion et les ressources statiques sont publiques ;
 * l'accueil exige lui aussi une connexion. Quatre comptes sont définis en mémoire,
 * leurs identifiants étant lus depuis la configuration (variables
 * {@code GESTOJOB_{LAURENT,RENEE,FT01,FT02}_{USERNAME,PASSWORD}} du fichier
 * {@code .env}) :</p>
 * <ul>
 *   <li>{@code laurent} et {@code renee} : rôle {@code TITULAIRE}, lecture et écriture
 *       sur leurs propres données ;</li>
 *   <li>{@code ft01} et {@code ft02} : rôle {@code LECTEUR}, consultation seule
 *       (respectivement des données de Laurent et de Renée).</li>
 * </ul>
 *
 * <p>Le lien compte / titulaire est écrit ici, sous forme d'autorités
 * {@code DONNEES_*}. La lecture seule est appliquée côté serveur : tout appel autre
 * que GET, et les formulaires de création, exigent le rôle {@code TITULAIRE}.</p>
 */
@Configuration
public class SecurityConfig {

    // Aucune valeur par défaut : l'application refuse de démarrer si les
    // identifiants ne sont pas fournis (évite tout identifiant codé en dur).
    @Value("${gestojob.comptes.laurent.username}")
    private String laurentUsername;

    @Value("${gestojob.comptes.laurent.password}")
    private String laurentPassword;

    @Value("${gestojob.comptes.renee.username}")
    private String reneeUsername;

    @Value("${gestojob.comptes.renee.password}")
    private String reneePassword;

    @Value("${gestojob.comptes.ft01.username}")
    private String ft01Username;

    @Value("${gestojob.comptes.ft01.password}")
    private String ft01Password;

    @Value("${gestojob.comptes.ft02.username}")
    private String ft02Username;

    @Value("${gestojob.comptes.ft02.password}")
    private String ft02Password;

    /**
     * Encodeur de mot de passe utilisé pour stocker et vérifier le mot de passe
     * des quatre comptes (laurent, renee, ft01, ft02).
     *
     * @return un encodeur BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Déclare les quatre comptes autorisés, dont les mots de passe en clair issus
     * de la configuration sont chiffrés en BCrypt au démarrage.
     *
     * @param passwordEncoder l'encodeur servant à chiffrer les mots de passe
     * @return un gestionnaire d'utilisateurs en mémoire contenant les quatre comptes
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                compte(passwordEncoder, laurentUsername, laurentPassword, "ROLE_TITULAIRE", "DONNEES_LAURENT"),
                compte(passwordEncoder, reneeUsername, reneePassword, "ROLE_TITULAIRE", "DONNEES_RENEE"),
                compte(passwordEncoder, ft01Username, ft01Password, "ROLE_LECTEUR", "DONNEES_LAURENT"),
                compte(passwordEncoder, ft02Username, ft02Password, "ROLE_LECTEUR", "DONNEES_RENEE"));
    }

    // Construit un compte en mémoire avec son rôle et son autorité de données.
    private static UserDetails compte(PasswordEncoder encodeur, String identifiant, String motDePasse,
                                      String role, String donnees) {
        return User.withUsername(identifiant)
                .password(encodeur.encode(motDePasse))
                .authorities(role, donnees)
                .build();
    }

    /**
     * Définit la chaîne de filtres de sécurité : règles d'accès, page de
     * connexion par formulaire et déconnexion. La protection CSRF reste active
     * (valeur par défaut de Spring Security).
     *
     * @param http le constructeur de configuration HTTP
     * @return la chaîne de filtres de sécurité
     * @throws Exception si la configuration échoue
     */
    // « throws Exception » est imposé par l'API Spring Security (HttpSecurity#build).
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        // Formulaires de création : GET, mais réservés aux titulaires.
                        .requestMatchers("/entreprises/ajout", "/entreprises/*/contacts/ajout")
                        .hasRole("TITULAIRE")
                        // Consultation ouverte à tout compte connecté, écriture aux titulaires.
                        .requestMatchers(HttpMethod.GET, "/**").authenticated()
                        .anyRequest().hasRole("TITULAIRE")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/entreprises", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        // Pas « / » : permitAll() ouvrirait aussi l'URL de sortie, or l'accueil est privé.
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        return http.build();
    }
}
