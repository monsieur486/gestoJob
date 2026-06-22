package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void getEntreprise_leveUneException_siIntrouvable() {
        when(entrepriseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entrepriseService.getEntreprise(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("introuvable");
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
