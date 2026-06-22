package com.mr486.gestojob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application Spring Boot GestoJob.
 * <p>
 * Cette classe démarre le contexte Spring et lance l'application de gestion
 * des candidatures.
 */
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
