package com.mr486.gestojob.configuration;

import com.mr486.gestojob.service.ModeleEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Amorce les modèles d'email au démarrage de l'application (création des modèles
 * manquants à partir de leurs valeurs par défaut, et chargement du cache).
 */
@Component
@RequiredArgsConstructor
public class ModeleEmailInitializer implements ApplicationRunner {

    private final ModeleEmailService modeleEmailService;

    @Override
    public void run(ApplicationArguments args) {
        modeleEmailService.initialiser();
    }
}
