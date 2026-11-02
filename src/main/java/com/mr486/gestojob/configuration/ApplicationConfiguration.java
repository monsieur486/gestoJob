package com.mr486.gestojob.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration générale de l'application.
 *
 * <p>Point unique de centralisation des <b>constantes métier</b> (options et
 * libellés relevant d'un choix fonctionnel) : salutation générique, civilités.
 * À distinguer de la configuration de production (secrets, hôtes, ports, tailles de page) qui provient du
 * fichier {@code .env}.
 */
@Configuration
public class ApplicationConfiguration {

    /** Salutation générique employée à défaut de civilité connue. */
    public static final String SALUTATION_GENERIQUE = "Madame, Monsieur,";

    /** Civilité « Madame » (préfixe d'une formule de politesse personnalisée). */
    public static final String CIVILITE_MADAME = "Madame";

    /** Civilité « Monsieur » (préfixe d'une formule de politesse personnalisée). */
    public static final String CIVILITE_MONSIEUR = "Monsieur";
}
