package com.mr486.gestojob.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Vérifie les contraintes du formulaire de saisie d'une offre consultée,
 * notamment le refus des liens non HTTP (un lien {@code javascript:} rendu
 * dans un {@code href} serait une faille XSS).
 */
class OffreConsulteeFormTest {

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

    private static OffreConsulteeForm formValide() {
        OffreConsulteeForm form = new OffreConsulteeForm();
        form.setDateConsultation(LocalDate.of(2026, 11, 18));
        form.setIntitule("Développeur Java senior");
        form.setEntreprise("Acme");
        form.setLien("https://exemple.fr/offre/42");
        form.setMotifEcart(1);
        form.setCommentaire("Exige 5 ans de Kotlin");
        return form;
    }

    private static Set<ConstraintViolation<OffreConsulteeForm>> valider(OffreConsulteeForm form) {
        return validator.validate(form);
    }

    @Test
    void formulaireComplet_estValide() {
        assertThat(valider(formValide())).isEmpty();
    }

    @Test
    void intituleBlanc_estInvalide() {
        OffreConsulteeForm form = formValide();
        form.setIntitule("  ");
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("intitule");
    }

    @Test
    void intituleTropLong_estInvalide() {
        OffreConsulteeForm form = formValide();
        form.setIntitule("x".repeat(256));
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("intitule");
    }

    @Test
    void entrepriseBlanche_estInvalide() {
        OffreConsulteeForm form = formValide();
        form.setEntreprise("");
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("entreprise");
    }

    @Test
    void dateAbsente_estInvalide() {
        OffreConsulteeForm form = formValide();
        form.setDateConsultation(null);
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("dateConsultation");
    }

    @Test
    void lienJavascript_estInvalide() {
        OffreConsulteeForm form = formValide();
        form.setLien("javascript:alert(1)");
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("lien");
    }

    @Test
    void lienVideOuAbsent_estValide() {
        OffreConsulteeForm form = formValide();
        form.setLien("");
        assertThat(valider(form)).isEmpty();
        form.setLien(null);
        assertThat(valider(form)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 4})
    void motifHorsListe_estInvalide(Integer motif) {
        OffreConsulteeForm form = formValide();
        form.setMotifEcart(motif);
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("motifEcart");
    }

    @Test
    void commentaireTropLong_estInvalide() {
        OffreConsulteeForm form = formValide();
        form.setCommentaire("x".repeat(1001));
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("commentaire");
    }

    @Test
    void lienEnMajusculesOuAvecEspacesDeBord_estValide() {
        OffreConsulteeForm form = formValide();
        form.setLien("HTTPS://exemple.fr/o/1");
        assertThat(valider(form)).isEmpty();
        form.setLien(" https://exemple.fr/o/1\n");
        assertThat(valider(form)).isEmpty();
        form.setLien("  ");
        assertThat(valider(form)).isEmpty();
    }

    @Test
    void lienJavascriptPrecedeDEspaces_resteInvalide() {
        OffreConsulteeForm form = formValide();
        form.setLien("  javascript:alert(1)");
        assertThat(valider(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("lien");
    }
}
