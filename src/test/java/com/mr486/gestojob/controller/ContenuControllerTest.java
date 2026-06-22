package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContenuControllerTest {

    @Mock
    private AnnonceService annonceService;

    @InjectMocks
    private ContenuController controller;

    @Test
    void getContenuById_renvoieLeContenuTexteDuService() {
        when(annonceService.getAnnonceTxtContenuById(3L)).thenReturn("Bonjour\n\nTexte");

        assertThat(controller.getContenuById(3L)).isEqualTo("Bonjour\n\nTexte");
    }
}
