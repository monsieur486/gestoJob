package com.mr486.gestojob.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que le formulaire d'annonce porte de vraies contraintes de validation
 * (l'annotation {@code @Valid} des contrôleurs serait sans effet sans elles).
 */
class AnnonceFormTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void entrepriseId_nullNeProduitPasDeViolation() {
        // entrepriseId est posé par le contrôleur depuis le chemin, APRÈS la
        // validation @Valid : il ne doit donc PAS être contraint sur le formulaire,
        // sinon la création d'annonce échoue silencieusement (régression connue).
        AnnonceForm form = AnnonceForm.builder().entrepriseId(null).poste("Développeur").build();

        Set<ConstraintViolation<AnnonceForm>> violations = validator.validate(form);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("entrepriseId"));
    }

    @Test
    void poste_tropLong_estRejete() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(7).poste("x".repeat(256)).build();

        Set<ConstraintViolation<AnnonceForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("poste"));
    }

    @Test
    void formulaireValide_aucuneViolation() {
        AnnonceForm form = AnnonceForm.builder()
                .entrepriseId(7).typeAnnonce(0).poste("Développeur").reference("REF-1").build();

        assertThat(validator.validate(form)).isEmpty();
    }
}
