package com.mr486.gestojob.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration générale de l'application.
 * <p>
 * Point unique de centralisation des <b>constantes métier</b> (options et
 * libellés relevant d'un choix fonctionnel) : nom du candidat, libellés de
 * courriel, formules de politesse. À distinguer de la configuration de
 * production (secrets, hôtes, ports, tailles de page) qui provient du
 * fichier {@code .env}.
 */
@Configuration
public class ApplicationConfiguration {

    /** Nom du candidat, réutilisé dans l'expéditeur et les libellés de courriel. */
    public static final String CANDIDAT_NOM = "Laurent Touret";

    /** Nom affiché par défaut comme expéditeur des courriels envoyés. */
    public static final String DEFAULT_EMAIL_FROM = CANDIDAT_NOM;

    /** Salutation générique employée à défaut de civilité connue. */
    public static final String SALUTATION_GENERIQUE = "Madame, Monsieur,";

    /** Civilité « Madame » (préfixe d'une formule de politesse personnalisée). */
    public static final String CIVILITE_MADAME = "Madame";

    /** Civilité « Monsieur » (préfixe d'une formule de politesse personnalisée). */
    public static final String CIVILITE_MONSIEUR = "Monsieur";

    /** Intitulé de poste par défaut, modèle généraliste. */
    public static final String POSTE_DEFAUT_GENERAL = "de développeur Java";

    /** Intitulé de poste par défaut, modèle microservices. */
    public static final String POSTE_DEFAUT_MICROSERVICES = "de développeur Java orienté microservices";

    /** Intitulé de poste par défaut, modèle IA agentique. */
    public static final String POSTE_DEFAUT_IA = "de développeur Java back-end orienté IA agentique";
}
