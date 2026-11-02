package com.mr486.gestojob.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mr486.gestojob.model.Profil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ProfilFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private ProfilForm valide() {
        return ProfilForm.builder().candidatNom("Renée")
                .libelleGeneral("Lettre 1").posteGeneral("p1")
                .libelleMicroservices("Lettre 2").posteMicroservices("p2")
                .libelleIa("Lettre 3").posteIa("p3").build();
    }

    @Test
    void formulaireValide_neLeveAucuneViolation() {
        assertThat(validator.validate(valide())).isEmpty();
    }

    @Test
    void champsVides_sontRefuses() {
        assertThat(validator.validate(new ProfilForm())).hasSize(7);
    }

    @Test
    void libelleTropLong_estRefuse() {
        ProfilForm form = valide();
        form.setLibelleIa("x".repeat(65));
        assertThat(validator.validate(form)).hasSize(1);
    }

    @Test
    void posteTropLong_estRefuse() {
        ProfilForm form = valide();
        form.setPosteGeneral("x".repeat(256));
        assertThat(validator.validate(form)).hasSize(1);
    }

    @Test
    void depuis_recopieLesChampsDuProfil() {
        Profil profil = Profil.builder().candidatNom("N").libelleGeneral("a").posteGeneral("b")
                .libelleMicroservices("c").posteMicroservices("d").libelleIa("e").posteIa("f").build();
        ProfilForm form = ProfilForm.depuis(profil);
        assertThat(form.getCandidatNom()).isEqualTo("N");
        assertThat(form.getPosteIa()).isEqualTo("f");
        assertThat(form.getLibelleMicroservices()).isEqualTo("c");
    }
}
