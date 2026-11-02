package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.EntrepriseListe;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EntrepriseRechercheServiceTest {

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @Mock
    private UtilisateurCourant utilisateurCourant;

    @InjectMocks
    private EntrepriseRechercheService entrepriseRechercheService;

    @BeforeEach
    void setUp() {
        lenient().when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
        ReflectionTestUtils.setField(entrepriseRechercheService, "maxEntreprisesParPage", 10);
    }

    private Entreprise entreprise(int id, String nom) {
        return Entreprise.builder().id(id).nom(nom).estActive(true).build();
    }

    @Test
    void getAllListePage_mappeLesEntreprisesEnDTO() {
        Page<Entreprise> page = new PageImpl<>(List.of(entreprise(1, "A"), entreprise(2, "B")));
        when(entrepriseRepository.findAllByTitulaireOrderByNomAsc(eq(Titulaire.RENEE), any(Pageable.class)))
                .thenReturn(page);

        Page<EntrepriseListe> result = entrepriseRechercheService.getAllListePage(0);

        assertThat(result.getContent()).extracting(EntrepriseListe::getNom).containsExactly("A", "B");
    }

    @Test
    void rechercheEntrepriseParNomPage_mappeLesResultats() {
        when(entrepriseRepository.findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc(
                eq(Titulaire.RENEE), eq("ac"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entreprise(1, "ACME"))));

        Page<EntrepriseListe> result = entrepriseRechercheService.rechercheEntrepriseParNomPage("ac", 0);

        assertThat(result.getContent()).extracting(EntrepriseListe::getNom).containsExactly("ACME");
    }

    @Test
    void rechercheEntrepriseActivePage_mappeLesResultats() {
        when(entrepriseRepository.findAllByTitulaireAndEstActiveTrueOrderByNomAsc(
                eq(Titulaire.RENEE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entreprise(1, "ACME"))));

        Page<EntrepriseListe> result = entrepriseRechercheService.rechercheEntrepriseActivePage(0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEstActive()).isTrue();
    }
}
