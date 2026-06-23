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
 * Vérifie la validation du formulaire de contact : l'email est facultatif au
 * niveau du formulaire (un contact « site » n'en a pas ; l'obligation, lorsqu'une
 * formule de politesse est choisie, est portée par le service), mais s'il est
 * fourni, son format doit être valide.
 */
class ContactFormTest {

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
    void emailAbsent_estTolere() {
        ContactForm form = ContactForm.builder().email(null).formuleDePolitesse(0).contact("Durand").build();

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void emailMalForme_estRejete() {
        ContactForm form = ContactForm.builder().email("pas-un-email").build();

        Set<ConstraintViolation<ContactForm>> violations = validator.validate(form);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}
