package com.mr486.gestojob.service;

import com.mr486.gestojob.model.Annonce;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibelleServiceTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    @InjectMocks
    private LibelleService libelleService;

    @Test
    void construitLibelle_reference_substitueLesVariables() {
        when(modeleEmailService.getContenu("LIBELLE_REFERENCE"))
                .thenReturn("Réf [{{REFERENCE}}] {{NOM}} - poste {{POSTE}}");
        Annonce annonce = Annonce.builder()
                .typeAnnonce(1).reference("ABC123").poste("Développeur").build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Réf [ABC123] Laurent Touret - poste Développeur");
    }

    @Test
    void construitLibelle_spontanee_utiliseLeModeleSpontanee() {
        when(modeleEmailService.getContenu("LIBELLE_SPONTANEE"))
                .thenReturn("{{NOM}} - spontanée");
        Annonce annonce = Annonce.builder().typeAnnonce(0).build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Laurent Touret - spontanée");
    }

    @Test
    void construitLibelle_referenceNulle_remplaceParChaineVide() {
        when(modeleEmailService.getContenu("LIBELLE_REFERENCE"))
                .thenReturn("Réf [{{REFERENCE}}] {{POSTE}}");
        Annonce annonce = Annonce.builder().typeAnnonce(1).build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Réf [] ");
    }
}
