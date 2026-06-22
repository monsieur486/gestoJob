package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import com.mr486.gestojob.service.EntrepriseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomePageControllerTest {

    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private AnnonceService annonceService;

    @InjectMocks
    private HomePageController controller;

    @Test
    void publicView_renvoieAccueil_etRemplitLeModele() {
        when(annonceService.getAllPositifListePage(0)).thenReturn(Page.empty());
        when(entrepriseService.countAllEntreprises()).thenReturn(3);
        when(annonceService.countAnnonces()).thenReturn(5L);

        Model model = new ExtendedModelMap();
        String view = controller.publicView(model, 1);

        assertThat(view).isEqualTo("accueil");
        assertThat(model.getAttribute("page_active")).isEqualTo("home");
        assertThat(model.getAttribute("nbrEntreprises")).isEqualTo("3 entreprises");
        assertThat(model.getAttribute("nbrAnnonces")).isEqualTo("5 annonces");
        assertThat(model.getAttribute("currentPage")).isEqualTo(1);
    }

    @Test
    void publicView_singulier_quandUnSeulElement() {
        when(annonceService.getAllPositifListePage(0)).thenReturn(Page.empty());
        when(entrepriseService.countAllEntreprises()).thenReturn(1);
        when(annonceService.countAnnonces()).thenReturn(1L);

        Model model = new ExtendedModelMap();
        controller.publicView(model, 1);

        assertThat(model.getAttribute("nbrEntreprises")).isEqualTo("1 entreprise");
        assertThat(model.getAttribute("nbrAnnonces")).isEqualTo("1 annonce");
    }
}
