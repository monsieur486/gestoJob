package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.ProfilForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.ProfilRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfilServiceTest {

    @Mock
    private ProfilRepository profilRepository;
    @Mock
    private UtilisateurCourant utilisateurCourant;
    @InjectMocks
    private ProfilService profilService;

    private Profil profil(Titulaire titulaire) {
        return Profil.builder().titulaire(titulaire).candidatNom("Nom")
                .libelleGeneral("Lg").posteGeneral("pg")
                .libelleMicroservices("Lm").posteMicroservices("pm")
                .libelleIa("Li").posteIa("pi").build();
    }

    @Test
    void posteParDefaut_microservices_renvoieLeChampMicroservices() {
        assertThat(profilService.posteParDefaut(profil(Titulaire.RENEE), CleModele.CONTENU_MICROSERVICES))
                .isEqualTo("pm");
        assertThat(profilService.posteParDefaut(profil(Titulaire.RENEE), CleModele.CONTENU_IA)).isEqualTo("pi");
        assertThat(profilService.posteParDefaut(profil(Titulaire.RENEE), CleModele.CONTENU_GENERAL)).isEqualTo("pg");
    }

    @Test
    void posteParDefaut_cleLibelle_leveIllegalArgument() {
        assertThatThrownBy(() -> profilService.posteParDefaut(profil(Titulaire.RENEE), CleModele.LIBELLE_REFERENCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void libelleContenu_general() {
        Profil p = profil(Titulaire.LAURENT);
        assertThat(profilService.libelleContenu(p, CleModele.CONTENU_GENERAL)).isEqualTo("Lg");
        assertThat(profilService.libelleContenu(p, CleModele.CONTENU_MICROSERVICES)).isEqualTo("Lm");
        assertThat(profilService.libelleContenu(p, CleModele.CONTENU_IA)).isEqualTo("Li");
    }

    @Test
    void libelleContenu_cleLibelle_leveIllegalArgument() {
        assertThatThrownBy(() -> profilService.libelleContenu(profil(Titulaire.RENEE), CleModele.LIBELLE_SPONTANEE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getProfil_absent_leveMetierException() {
        when(profilRepository.findById(Titulaire.RENEE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> profilService.getProfil(Titulaire.RENEE)).isInstanceOf(MetierException.class);
    }

    @Test
    void getProfilCourant_utiliseLeTitulaireCourant() {
        Profil p = profil(Titulaire.RENEE);
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
        when(profilRepository.findById(Titulaire.RENEE)).thenReturn(Optional.of(p));
        assertThat(profilService.getProfilCourant()).isSameAs(p);
    }

    @Test
    void mettreAJour_enregistreSurLeTitulaireCourant() {
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
        when(profilRepository.findById(Titulaire.RENEE)).thenReturn(Optional.of(profil(Titulaire.RENEE)));
        ProfilForm form = ProfilForm.depuis(profil(Titulaire.RENEE));
        form.setCandidatNom("  Renée Durand ");

        profilService.mettreAJour(form);

        ArgumentCaptor<Profil> captor = ArgumentCaptor.forClass(Profil.class);
        verify(profilRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulaire()).isEqualTo(Titulaire.RENEE);
        assertThat(captor.getValue().getCandidatNom()).isEqualTo("Renée Durand");
    }
}
