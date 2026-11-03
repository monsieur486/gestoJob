package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.ModeleEmail;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.ModeleEmailRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModeleEmailServiceTest {

    @Mock
    private ModeleEmailRepository modeleEmailRepository;
    @Mock
    private UtilisateurCourant utilisateurCourant;
    @Mock
    private ProfilService profilService;

    @InjectMocks
    private ModeleEmailService modeleEmailService;

    @BeforeEach
    void setUp() {
        lenient().when(modeleEmailRepository.findByTitulaireAndCle(any(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(utilisateurCourant.titulaire()).thenReturn(Titulaire.LAURENT);
    }

    @Test
    void initialiser_creeLesModelesAbsentsPourChaqueTitulaire() {
        modeleEmailService.initialiser();

        verify(modeleEmailRepository, times(2 * CleModele.values().length)).save(any(ModeleEmail.class));
        assertThat(modeleEmailService.getContenu(Titulaire.LAURENT, "LIBELLE_SPONTANEE"))
                .contains("Candidature spontanée");
    }

    @Test
    void initialiser_neSolliciteJamaisLUtilisateurCourant() {
        modeleEmailService.initialiser();

        org.mockito.Mockito.verifyNoInteractions(utilisateurCourant);
    }

    @Test
    void initialiser_attribueLeBonTitulaireAChaqueModele() {
        modeleEmailService.initialiser();

        ArgumentCaptor<ModeleEmail> captor = ArgumentCaptor.forClass(ModeleEmail.class);
        verify(modeleEmailRepository, times(2 * CleModele.values().length)).save(captor.capture());
        assertThat(captor.getAllValues().stream().filter(m -> m.getTitulaire() == Titulaire.RENEE).count())
                .isEqualTo(CleModele.values().length);
        assertThat(captor.getAllValues()).allMatch(m -> m.getTitulaire() != null);
    }

    @Test
    void getContenu_cleInconnue_leveMetierException() {
        assertThatThrownBy(() -> modeleEmailService.getContenu(Titulaire.LAURENT, "INCONNU"))
                .isInstanceOf(MetierException.class);
    }

    @Test
    void getContenu_isoleLeCacheParTitulaire() {
        String laurent = modeleEmailService.getContenu(Titulaire.LAURENT, "CONTENU_GENERAL");
        String renee = modeleEmailService.getContenu(Titulaire.RENEE, "CONTENU_GENERAL");

        assertThat(laurent).isNotEqualTo(renee);
        assertThat(renee).doesNotContain("Laurent");
    }

    @Test
    void mettreAJour_nettoieLeHtml_etMetAJourLeCacheDuTitulaireCourant() {
        modeleEmailService.mettreAJour("CONTENU_GENERAL", "<p>{{POLITESSE}} {{POSTE}}</p><script>x</script>");

        String courant = modeleEmailService.getContenu(Titulaire.LAURENT, "CONTENU_GENERAL");
        assertThat(courant).contains("{{POLITESSE}}").doesNotContain("script");
        ArgumentCaptor<ModeleEmail> captor = ArgumentCaptor.forClass(ModeleEmail.class);
        verify(modeleEmailRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulaire()).isEqualTo(Titulaire.LAURENT);
    }

    @Test
    void mettreAJour_modeleExistant_leModifieSansChangerSonTitulaire() {
        ModeleEmail existant = ModeleEmail.builder().cle("LIBELLE_REFERENCE")
                .titulaire(Titulaire.LAURENT).contenu("ancien").build();
        when(modeleEmailRepository.findByTitulaireAndCle(Titulaire.LAURENT, "LIBELLE_REFERENCE"))
                .thenReturn(Optional.of(existant));

        modeleEmailService.mettreAJour("LIBELLE_REFERENCE", "  Réf {{REFERENCE}} - {{NOM}} ");

        assertThat(existant.getContenu()).isEqualTo("Réf {{REFERENCE}} - {{NOM}}");
        assertThat(modeleEmailService.getContenu(Titulaire.LAURENT, "LIBELLE_REFERENCE"))
                .isEqualTo("Réf {{REFERENCE}} - {{NOM}}");
    }

    @Test
    void mettreAJour_lettreSansPoste_refuseEtNEnregistreRien() {
        assertThatThrownBy(() -> modeleEmailService.mettreAJour("CONTENU_GENERAL", "<p>{{POLITESSE}}</p>"))
                .isInstanceOf(MetierException.class)
                .hasMessageContaining("{{POSTE}}")
                .hasMessageNotContaining("{{POLITESSE}}");
        verify(modeleEmailRepository, never()).save(any(ModeleEmail.class));
    }

    @Test
    void mettreAJour_lettreSansAucuneVariable_citeLesDeux() {
        assertThatThrownBy(() -> modeleEmailService.mettreAJour("CONTENU_IA", "<p>Bonjour</p>"))
                .isInstanceOf(MetierException.class)
                .hasMessageContaining("{{POLITESSE}}")
                .hasMessageContaining("{{POSTE}}");
    }

    @Test
    void mettreAJour_lettreNulle_refusee() {
        assertThatThrownBy(() -> modeleEmailService.mettreAJour("CONTENU_GENERAL", null))
                .isInstanceOf(MetierException.class);
    }

    @Test
    void mettreAJour_cleInconnue_leveMetierException() {
        assertThatThrownBy(() -> modeleEmailService.mettreAJour("INCONNU", "x"))
                .isInstanceOf(MetierException.class);
    }

    @Test
    void reinitialiser_restaureLaValeurParDefaut() {
        modeleEmailService.reinitialiser("LIBELLE_SPONTANEE");

        assertThat(modeleEmailService.getContenu(Titulaire.LAURENT, "LIBELLE_SPONTANEE"))
                .contains("Candidature spontanée");
    }

    @Test
    void reinitialiser_rechargeLeDefautDuTitulaireCourant() {
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);

        modeleEmailService.reinitialiser("CONTENU_GENERAL");

        ArgumentCaptor<ModeleEmail> captor = ArgumentCaptor.forClass(ModeleEmail.class);
        verify(modeleEmailRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulaire()).isEqualTo(Titulaire.RENEE);
        assertThat(captor.getValue().getContenu()).doesNotContain("Laurent").contains("{{POSTE}}");
    }

    @Test
    void listerModeles_retourneLesCinqModelesDuTitulaireCourant_libellesDuProfil() {
        Profil profil = Profil.builder().titulaire(Titulaire.LAURENT).libelleGeneral("Lg")
                .libelleMicroservices("Lm").libelleIa("Li").build();
        when(profilService.getProfilCourant()).thenReturn(profil);
        when(profilService.libelleContenu(profil, CleModele.CONTENU_GENERAL)).thenReturn("Lg");
        when(profilService.libelleContenu(profil, CleModele.CONTENU_MICROSERVICES)).thenReturn("Lm");
        when(profilService.libelleContenu(profil, CleModele.CONTENU_IA)).thenReturn("Li");

        var vues = modeleEmailService.listerModeles();

        assertThat(vues).hasSize(5);
        assertThat(vues.get(0).getLibelleUi()).isEqualTo("Lg");
        assertThat(vues.get(4).getLibelleUi()).isEqualTo(CleModele.LIBELLE_SPONTANEE.getLibelleUi());
        assertThat(vues.get(0).getVariablesObligatoires()).containsExactly("{{POLITESSE}}", "{{POSTE}}");
        assertThat(vues.get(4).getVariablesObligatoires()).isEmpty();
    }
}
