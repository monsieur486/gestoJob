package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.dto.EntrepriseListe;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceTest {

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @InjectMocks
    private EntrepriseService entrepriseService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(entrepriseService, "maxEntreprisesParPage", 10);
    }

    private Entreprise entreprise(int id, String nom) {
        return Entreprise.builder().id(id).nom(nom).estActive(true).build();
    }

    @Test
    void save_leveUneException_siNomDejaExistant() {
        EntrepriseForm form = EntrepriseForm.builder().nom("ACME").build();
        when(entrepriseRepository.existsByNomIgnoreCase("ACME")).thenReturn(true);

        assertThatThrownBy(() -> entrepriseService.save(form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe déjà");

        verify(entrepriseRepository, never()).save(any());
    }

    @Test
    void save_retourneLId_siNouvelleEntreprise() {
        EntrepriseForm form = EntrepriseForm.builder().nom("ACME").build();
        when(entrepriseRepository.existsByNomIgnoreCase("ACME")).thenReturn(false);
        when(entrepriseRepository.save(any())).thenReturn(entreprise(42, "ACME"));

        assertThat(entrepriseService.save(form)).isEqualTo(42);
    }

    @Test
    void update_modifieEtEnregistre_siEntrepriseExiste() {
        Entreprise e = entreprise(5, "Ancien");
        when(entrepriseRepository.findById(5)).thenReturn(Optional.of(e));
        EntrepriseForm form = EntrepriseForm.builder().nom("Nouveau").ville("Lyon").codePostal("69000").build();

        entrepriseService.update(5, form);

        ArgumentCaptor<Entreprise> captor = ArgumentCaptor.forClass(Entreprise.class);
        verify(entrepriseRepository).save(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("Nouveau");
        assertThat(captor.getValue().getVille()).isEqualTo("Lyon");
    }

    @Test
    void update_leveUneException_siEntrepriseIntrouvable() {
        when(entrepriseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.update(99, new EntrepriseForm()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void getForm_renvoieLeFormulairePrerempli() {
        when(entrepriseRepository.findById(5)).thenReturn(Optional.of(entreprise(5, "ACME")));

        EntrepriseForm form = entrepriseService.getForm(5);

        assertThat(form.getNom()).isEqualTo("ACME");
    }

    @Test
    void getEntreprise_leveUneException_siIntrouvable() {
        when(entrepriseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.getEntreprise(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void activeEntreprise_passeEstActiveAVrai() {
        Entreprise e = Entreprise.builder().id(5).nom("ACME").estActive(false).build();
        when(entrepriseRepository.findById(5)).thenReturn(Optional.of(e));

        entrepriseService.activeEntreprise(5);

        assertThat(e.getEstActive()).isTrue();
        verify(entrepriseRepository).save(e);
    }

    @Test
    void desactiveEntreprise_passeEstActiveAFaux() {
        Entreprise e = Entreprise.builder().id(5).nom("ACME").estActive(true).build();
        when(entrepriseRepository.findById(5)).thenReturn(Optional.of(e));

        entrepriseService.desactiveEntreprise(5);

        assertThat(e.getEstActive()).isFalse();
        verify(entrepriseRepository).save(e);
    }

    @Test
    void getAllListePage_mappeLesEntreprisesEnDTO() {
        Page<Entreprise> page = new PageImpl<>(List.of(entreprise(1, "A"), entreprise(2, "B")));
        when(entrepriseRepository.findAllByOrderByNomAsc(any(Pageable.class))).thenReturn(page);

        Page<EntrepriseListe> result = entrepriseService.getAllListePage(0);

        assertThat(result.getContent()).extracting(EntrepriseListe::getNom).containsExactly("A", "B");
    }

    @Test
    void rechercheEntrepriseParNom_mappeLesResultats() {
        when(entrepriseRepository.findAllByNomContainingIgnoreCase("ac"))
                .thenReturn(List.of(entreprise(1, "ACME")));

        List<EntrepriseListe> result = entrepriseService.rechercheEntrepriseParNom("ac");

        assertThat(result).extracting(EntrepriseListe::getNom).containsExactly("ACME");
    }

    @Test
    void rechercheEntrepriseActive_mappeLesResultats() {
        when(entrepriseRepository.findAllByEstActiveTrue())
                .thenReturn(List.of(entreprise(1, "ACME")));

        List<EntrepriseListe> result = entrepriseService.rechercheEntrepriseActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEstActive()).isTrue();
    }

    @Test
    void existe_delegueAuRepository() {
        when(entrepriseRepository.existsById(5)).thenReturn(true);
        assertThat(entrepriseService.existe(5)).isTrue();
    }

    @Test
    void countAllEntreprises_renvoieLeNombre() {
        when(entrepriseRepository.count()).thenReturn(7L);
        assertThat(entrepriseService.countAllEntreprises()).isEqualTo(7);
    }

    @Test
    void convert_recopieLesChampsDuFormulaireVersLEntite() {
        EntrepriseForm form = EntrepriseForm.builder()
                .nom("ACME").estActive(true).adresse1("1 rue X").adresse2("Bât. B")
                .codePostal("67000").ville("Strasbourg").build();
        Entreprise e = new Entreprise();

        EntrepriseService.Convert(form, e);

        assertThat(e.getNom()).isEqualTo("ACME");
        assertThat(e.getCodePostal()).isEqualTo("67000");
        assertThat(e.getVille()).isEqualTo("Strasbourg");
        assertThat(e.getEstActive()).isTrue();
    }

    @Test
    void getEntreprisesByIds_renvoieUneMapIndexeeParId() {
        when(entrepriseRepository.findAllById(List.of(1, 2)))
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
        verify(entrepriseRepository, never()).findAllById(any());
    }
}
