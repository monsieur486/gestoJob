package com.mr486.gestojob.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * <p>
 * La page d'accueil et les ressources statiques restent publiques ; toutes les
 * autres pages exigent une authentification par formulaire. Un unique compte
 * administrateur est défini en mémoire, ses identifiants étant lus depuis la
 * configuration (variables {@code GESTOJOB_ADMIN_USERNAME} /
 * {@code GESTOJOB_ADMIN_PASSWORD} du fichier {@code .env}).
 */
@Configuration
public class SecurityConfig {

    // Aucune valeur par défaut : l'application refuse de démarrer si les
    // identifiants ne sont pas fournis (évite tout identifiant codé en dur).
    @Value("${gestojob.admin.username}")
    private String adminUsername;

    @Value("${gestojob.admin.password}")
    private String adminPassword;

    /**
     * Encodeur de mot de passe utilisé pour stocker et vérifier le mot de passe
     * du compte administrateur.
     *
     * @return un encodeur BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Déclare l'unique utilisateur autorisé, dont le mot de passe en clair issu
     * de la configuration est chiffré en BCrypt au démarrage.
     *
     * @param passwordEncoder l'encodeur servant à chiffrer le mot de passe
     * @return un gestionnaire d'utilisateurs en mémoire contenant le compte admin
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Définit la chaîne de filtres de sécurité : pages publiques, page de
     * connexion par formulaire et déconnexion. La protection CSRF reste active
     * (valeur par défaut de Spring Security).
     *
     * @param http le constructeur de configuration HTTP
     * @return la chaîne de filtres de sécurité
     * @throws Exception si la configuration échoue
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/entreprises", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );
        return http.build();
    }
}
