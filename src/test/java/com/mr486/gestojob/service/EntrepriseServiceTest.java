package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceTest {

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @Mock
    private UtilisateurCourant utilisateurCourant;

    @InjectMocks
    private EntrepriseService entrepriseService;

    @BeforeEach
    void setUp() {
        lenient().when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
    }

    private Entreprise entreprise(int id, String nom) {
        return Entreprise.builder().id(id).nom(nom).estActive(true).build();
    }

    @Test
    void save_leveUneException_siNomDejaExistant() {
        EntrepriseForm form = EntrepriseForm.builder().nom("ACME").build();
        when(entrepriseRepository.existsByTitulaireAndNomIgnoreCase(Titulaire.RENEE, "ACME")).thenReturn(true);

        assertThatThrownBy(() -> entrepriseService.save(form))
                .isInstanceOf(MetierException.class)
                .hasMessageContaining("existe déjà");

        verify(entrepriseRepository, never()).save(any());
    }

    @Test
    void save_retourneLId_siNouvelleEntreprise() {
        EntrepriseForm form = EntrepriseForm.builder().nom("ACME").build();
        when(entrepriseRepository.existsByTitulaireAndNomIgnoreCase(Titulaire.RENEE, "ACME")).thenReturn(false);
        when(entrepriseRepository.save(any())).thenReturn(entreprise(42, "ACME"));

        assertThat(entrepriseService.save(form)).isEqualTo(42);
    }

    @Test
    void update_modifieEtEnregistre_siEntrepriseExiste() {
        Entreprise e = entreprise(5, "Ancien");
        when(entrepriseRepository.findByIdAndTitulaire(5, Titulaire.RENEE)).thenReturn(Optional.of(e));
        EntrepriseForm form = EntrepriseForm.builder().nom("Nouveau").ville("Lyon").codePostal("69000").build();

        entrepriseService.update(5, form);

        ArgumentCaptor<Entreprise> captor = ArgumentCaptor.forClass(Entreprise.class);
        verify(entrepriseRepository).save(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("Nouveau");
        assertThat(captor.getValue().getVille()).isEqualTo("Lyon");
    }

    @Test
    void update_leveUneException_siEntrepriseIntrouvable() {
        when(entrepriseRepository.findByIdAndTitulaire(99, Titulaire.RENEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.update(99, new EntrepriseForm()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void update_propageLErreur_siEchecDePersistance() {
        Entreprise e = entreprise(5, "Ancien");
        when(entrepriseRepository.findByIdAndTitulaire(5, Titulaire.RENEE)).thenReturn(Optional.of(e));
        when(entrepriseRepository.save(any())).thenThrow(new IllegalStateException("erreur SGBD"));
        EntrepriseForm form = EntrepriseForm.builder().nom("Nouveau").build();

        // L'erreur réelle de persistance ne doit PAS être masquée en « introuvable ».
        assertThatThrownBy(() -> entrepriseService.update(5, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SGBD");
    }

    @Test
    void getForm_renvoieLeFormulairePrerempli() {
        when(entrepriseRepository.findByIdAndTitulaire(5, Titulaire.RENEE))
                .thenReturn(Optional.of(entreprise(5, "ACME")));

        EntrepriseForm form = entrepriseService.getForm(5);

        assertThat(form.getNom()).isEqualTo("ACME");
    }

    @Test
    void getEntreprise_leveUneException_siIntrouvable() {
        when(entrepriseRepository.findByIdAndTitulaire(99, Titulaire.RENEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.getEntreprise(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void setActive_true_passeEstActiveAVrai() {
        Entreprise e = Entreprise.builder().id(5).nom("ACME").estActive(false).build();
        when(entrepriseRepository.findByIdAndTitulaire(5, Titulaire.RENEE)).thenReturn(Optional.of(e));

        entrepriseService.setActive(5, true);

        assertThat(e.getEstActive()).isTrue();
        verify(entrepriseRepository).save(e);
    }

    @Test
    void setActive_false_passeEstActiveAFaux() {
        Entreprise e = Entreprise.builder().id(5).nom("ACME").estActive(true).build();
        when(entrepriseRepository.findByIdAndTitulaire(5, Titulaire.RENEE)).thenReturn(Optional.of(e));

        entrepriseService.setActive(5, false);

        assertThat(e.getEstActive()).isFalse();
        verify(entrepriseRepository).save(e);
    }

    @Test
    void existe_delegueAuRepository() {
        when(entrepriseRepository.existsByIdAndTitulaire(5, Titulaire.RENEE)).thenReturn(true);
        assertThat(entrepriseService.existe(5)).isTrue();
    }

    @Test
    void countAllEntreprises_renvoieLeNombre() {
        when(entrepriseRepository.countByTitulaire(Titulaire.RENEE)).thenReturn(7L);
        assertThat(entrepriseService.countAllEntreprises()).isEqualTo(7);
    }

    @Test
    void getEntreprisesByIds_renvoieUneMapIndexeeParId() {
        when(entrepriseRepository.findAllByIdInAndTitulaire(List.of(1, 2), Titulaire.RENEE))
                .thenReturn(List.of(entreprise(1, "A"), entreprise(2, "B")));

        Map<Integer, Entreprise> result = entrepriseService.getEntreprisesByIds(List.of(1, 2));

        assertThat(result).hasSize(2);
        assertThat(result.get(1).getNom()).isEqualTo("A");
        assertThat(result.get(2).getNom()).isEqualTo("B");
    }

    @Test
    void getEntreprisesByIds_renvoieMapVide_siAucunId_sansAppelerLeRepo() {
        Map<Integer, Entreprise> result = entrepriseService.getEntreprisesByIds(List.of());

        assertThat(result).isEmpty();
        verify(entrepriseRepository, never()).findAllByIdInAndTitulaire(any(), any());
    }

    @Test
    void getEntreprise_idAutreTitulaire_seComporteCommeInconnu() {
        when(entrepriseRepository.findByIdAndTitulaire(5, Titulaire.RENEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.getEntreprise(5))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void save_poseLeTitulaireCourant() {
        EntrepriseForm form = EntrepriseForm.builder().nom("ACME").build();
        when(entrepriseRepository.existsByTitulaireAndNomIgnoreCase(Titulaire.RENEE, "ACME")).thenReturn(false);
        when(entrepriseRepository.save(any())).thenReturn(entreprise(42, "ACME"));

        entrepriseService.save(form);

        ArgumentCaptor<Entreprise> captor = ArgumentCaptor.forClass(Entreprise.class);
        verify(entrepriseRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulaire()).isEqualTo(Titulaire.RENEE);
    }

    @Test
    void save_nomDejaPrisParLAutreTitulaire_accepte() {
        EntrepriseForm form = EntrepriseForm.builder().nom("Acme").build();
        when(entrepriseRepository.existsByTitulaireAndNomIgnoreCase(Titulaire.RENEE, "Acme")).thenReturn(false);
        when(entrepriseRepository.save(any())).thenReturn(entreprise(8, "Acme"));

        assertThat(entrepriseService.save(form)).isEqualTo(8);
        verify(entrepriseRepository).save(any());
    }

    @Test
    void countAllEntreprises_compteLeTitulaireCourant() {
        when(entrepriseRepository.countByTitulaire(Titulaire.RENEE)).thenReturn(3L);

        assertThat(entrepriseService.countAllEntreprises()).isEqualTo(3);
        verify(entrepriseRepository, never()).count();
    }

    @Test
    void setActive_idAutreTitulaire_neModifieRien() {
        when(entrepriseRepository.findByIdAndTitulaire(5, Titulaire.RENEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.setActive(5, true))
                .isInstanceOf(NoSuchElementException.class);

        verify(entrepriseRepository, never()).save(any());
    }
}
