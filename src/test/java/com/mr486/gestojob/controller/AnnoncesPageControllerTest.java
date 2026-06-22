package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.RechercheAnnonceForm;
import com.mr486.gestojob.service.AnnonceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnoncesPageControllerTest {

    @Mock
    private AnnonceService annonceService;

    @InjectMocks
    private AnnoncesPageController controller;

    @Test
    void annoncesView_construitLaRechercheEtRemplitLeModele() {
        when(annonceService.searchAnnoncesPage(any(RechercheAnnonceForm.class), eq(0)))
                .thenReturn(Page.empty());

        Model model = new ExtendedModelMap();
        String view = controller.annoncesView(model, 1, "java", true);

        assertThat(view).isEqualTo("annonces");
        assertThat(model.getAttribute("page_active")).isEqualTo("annonces");
        assertThat(model.getAttribute("currentPage")).isEqualTo(1);
        assertThat(model.getAttribute("searchQuery")).isEqualTo("java");
        assertThat(model.getAttribute("includeArchives")).isEqualTo(true);
        assertThat(model.getAttribute("searchForm")).isInstanceOf(RechercheAnnonceForm.class);
    }

    @Test
    void postNegatif_appelleSetRefus_etRedirige() {
        assertThat(controller.postReponse(7L)).isEqualTo("redirect:/annonces");
        verify(annonceService).setRefus(7L);
    }

    @Test
    void postPositif_appelleSetAccepte_etRedirige() {
        assertThat(controller.postReponseAccepte(7L)).isEqualTo("redirect:/annonces");
        verify(annonceService).setAccepte(7L);
    }
}
