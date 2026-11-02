package com.mr486.gestojob.journal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Classement des exceptions remontées au conteneur web, cas « ressource métier introuvable ». */
class ClassementErreurTest {

    @Test
    void noSuchElement_estClasseIntrouvableEnInfoSansTrace() {
        ClassementErreur.Classement classement = ClassementErreur.classer(
                new NoSuchElementException("Entreprise introuvable avec id: 9"),
                "/entreprises/9", null, "gestojob.mr486.com");

        assertThat(classement.type()).isEqualTo("appli.http.introuvable");
        assertThat(classement.niveau()).isEqualTo("info");
        assertThat(classement.resume()).isEqualTo("Ressource introuvable : /entreprises/9");
        assertThat(classement.avecTrace()).isFalse();
    }

    @Test
    void noSuchElement_avecRefererInterne_resteIntrouvable_pasLienCasse() {
        // Un id étranger ou périmé cliqué depuis une page du site n'est pas un lien cassé de l'appli.
        ClassementErreur.Classement classement = ClassementErreur.classer(
                new NoSuchElementException("x"), "/annonces/4/positif",
                "https://gestojob.mr486.com/annonces", "gestojob.mr486.com");

        assertThat(classement.type()).isEqualTo("appli.http.introuvable");
        assertThat(classement.niveau()).isEqualTo("info");
    }

    @Test
    void uneExceptionOrdinaire_resteUneErreurAvecTrace() {
        ClassementErreur.Classement classement = ClassementErreur.classer(
                new IllegalStateException("boum"), "/x", null, null);

        assertThat(classement.type()).isEqualTo("appli.erreur");
        assertThat(classement.avecTrace()).isTrue();
    }
}
