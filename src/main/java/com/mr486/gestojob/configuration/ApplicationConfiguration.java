package com.mr486.gestojob.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration générale de l'application.
 * <p>
 * Regroupe les constantes globales utilisées par l'application, notamment
 * les libellés par défaut relatifs aux courriels de candidature.
 */
@Configuration
public class ApplicationConfiguration {

    /** Nom affiché par défaut comme expéditeur des courriels envoyés. */
    public static final String DEFAULT_EMAIL_FROM = "Laurent Touret";

    /** Objet par défaut utilisé pour les courriels de candidature spontanée. */
    public static final String DEMANDE_SPONTANEE_TXT =
            "Laurent Touret - Candidature spontanée pour un poste de développeur Java - Springboot";
}
