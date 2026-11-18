package com.mr486.gestojob.dto.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnnonceDemandeTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void typeAnnonce_zeroEtUn_acceptes() {
        assertThat(validator.validate(new AnnonceDemande(0, "Dev", null))).isEmpty();
        assertThat(validator.validate(new AnnonceDemande(1, "Dev", null))).isEmpty();
    }

    @Test
    void typeAnnonce_horsBornes_refuse() {
        Set<ConstraintViolation<AnnonceDemande>> negatif = validator.validate(new AnnonceDemande(-1, "Dev", null));
        Set<ConstraintViolation<AnnonceDemande>> trop = validator.validate(new AnnonceDemande(2, "Dev", null));

        assertThat(negatif).extracting(ConstraintViolation::getMessage)
                .containsExactly("Le type d'annonce doit valoir 0 (spontanée) ou 1 (référence).");
        assertThat(trop).extracting(ConstraintViolation::getMessage)
                .containsExactly("Le type d'annonce doit valoir 0 (spontanée) ou 1 (référence).");
    }

    @Test
    void typeAnnonce_nul_refuse() {
        assertThat(validator.validate(new AnnonceDemande(null, "Dev", null)))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Le type d'annonce est obligatoire.");
    }
}
