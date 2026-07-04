package com.mr486.gestojob.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.service.AnnonceTexteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContenuControllerTest {

    @Mock
    private AnnonceTexteService annonceTexteService;

    @InjectMocks
    private ContenuController controller;

    @Test
    void getContenuById_renvoieLeContenuTexteDuService() {
        when(annonceTexteService.getAnnonceTxtContenuById(3L)).thenReturn("Bonjour\n\nTexte");

        assertThat(controller.getContenuById(3L)).isEqualTo("Bonjour\n\nTexte");
    }
}
