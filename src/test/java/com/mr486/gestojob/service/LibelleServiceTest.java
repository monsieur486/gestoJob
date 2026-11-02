package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibelleServiceTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    @Mock
    private ProfilService profilService;

    @InjectMocks
    private LibelleService libelleService;

    @BeforeEach
    void setUp() {
        lenient().when(profilService.getProfil(Titulaire.LAURENT))
                .thenReturn(Profil.builder().titulaire(Titulaire.LAURENT).candidatNom("Laurent Touret").build());
    }

    @Test
    void construitLibelle_utiliseLeNomDuProfilDeLAnnonce() {
        when(profilService.getProfil(Titulaire.RENEE))
                .thenReturn(Profil.builder().titulaire(Titulaire.RENEE).candidatNom("Renée").build());
        when(modeleEmailService.getContenu(Titulaire.RENEE, "LIBELLE_SPONTANEE"))
                .thenReturn("{{NOM}} - Candidature spontanée");
        Annonce annonce = Annonce.builder().titulaire(Titulaire.RENEE).typeAnnonce(0).build();

        assertThat(libelleService.construitLibelle(annonce)).startsWith("Renée - ");
    }

    @Test
    void construitLibelle_reference_substitueLesVariables() {
        when(modeleEmailService.getContenu(Titulaire.LAURENT, "LIBELLE_REFERENCE"))
                .thenReturn("Réf [{{REFERENCE}}] {{NOM}} - poste {{POSTE}}");
        Annonce annonce = Annonce.builder()
                .titulaire(Titulaire.LAURENT).typeAnnonce(1).reference("ABC123").poste("Développeur").build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Réf [ABC123] Laurent Touret - poste Développeur");
    }

    @Test
    void construitLibelle_spontanee_utiliseLeModeleSpontanee() {
        when(modeleEmailService.getContenu(Titulaire.LAURENT, "LIBELLE_SPONTANEE"))
                .thenReturn("{{NOM}} - spontanée");
        Annonce annonce = Annonce.builder().titulaire(Titulaire.LAURENT).typeAnnonce(0).build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Laurent Touret - spontanée");
    }

    @Test
    void construitLibelle_referenceNulle_remplaceParChaineVide() {
        when(modeleEmailService.getContenu(Titulaire.LAURENT, "LIBELLE_REFERENCE"))
                .thenReturn("Réf [{{REFERENCE}}] {{POSTE}}");
        Annonce annonce = Annonce.builder().titulaire(Titulaire.LAURENT).typeAnnonce(1).build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Réf [] ");
    }
}
