package com.mr486.gestojob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application Spring Boot GestoJob.
 *
 * <p>Cette classe démarre le contexte Spring et lance l'application de gestion
 * des candidatures.
 */
// Faux positif PMD : classe de démarrage Spring, pas une classe utilitaire
// (un constructeur privé casserait le proxy CGLIB de @Configuration).
@SuppressWarnings("PMD.UseUtilityClass")
@SpringBootApplication
public class GestoJobApplication {

    /**
     * Démarre l'application Spring Boot.
     *
     * @param args les arguments de la ligne de commande transmis à l'application
     */
    public static void main(String[] args) {
        SpringApplication.run(GestoJobApplication.class, args);
    }

}
