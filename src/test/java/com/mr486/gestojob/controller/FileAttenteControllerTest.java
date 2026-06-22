package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileAttenteControllerTest {

    @Mock
    private AnnonceService annonceService;

    @InjectMocks
    private FileAttenteController controller;

    @Test
    void fileAttenteView_renvoieFile_etChargeLesAnnoncesEnAttente() {
        when(annonceService.annoncesEnAttenteEnvoiEmail()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.fileAttenteView(model);

        assertThat(view).isEqualTo("file");
        assertThat(model.getAttribute("page_active")).isEqualTo("file");
        assertThat(model.getAttribute("annoncesEnAttenteEnvoiEmail")).isEqualTo(List.of());
    }

    @Test
    void postMail_envoieToutesLesAnnoncesEnAttente_etRedirige() {
        assertThat(controller.postMail()).isEqualTo("redirect:/file");
        verify(annonceService).sendEmailForPendingAnnonces();
    }

    @Test
    void postDirectMail_envoieUneAnnonce_etRedirige() {
        assertThat(controller.postDirectMail(9L)).isEqualTo("redirect:/file");
        verify(annonceService).sendDirectEmail(9L);
    }
}
