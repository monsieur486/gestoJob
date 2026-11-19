package com.mr486.gestojob.dto.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/**
 * Vérifie les contraintes de la demande d'API d'enregistrement d'une offre
 * consultée (mêmes règles que le formulaire de la page).
 */
class OffreConsulteeDemandeTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static OffreConsulteeDemande demande(String intitule, String lien, String motif) {
        return new OffreConsulteeDemande(intitule, "Acme", lien, motif, "Lyon", null);
    }

    @Test
    void demandeComplete_estValide() {
        assertThat(validator.validate(demande("Dév Java", "https://exemple.fr/o/1", "DISTANCE"))).isEmpty();
        assertThat(validator.validate(demande("Dév Java", null, "profil"))).isEmpty();
    }

    @Test
    void lienJavascript_estRefuse() {
        assertThat(validator.validate(demande("Dév Java", "javascript:alert(1)", "PROFIL")))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("lien");
    }

    @Test
    void intituleBlanc_estRefuse() {
        assertThat(validator.validate(demande(" ", null, "PROFIL")))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("intitule");
    }

    @Test
    void motifBlanc_estRefuse() {
        assertThat(validator.validate(demande("Dév Java", null, "")))
                .extracting(ConstraintViolation::getMessage).containsExactly("Le motif est obligatoire.");
    }
}
