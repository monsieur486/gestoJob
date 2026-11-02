package com.mr486.gestojob.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.service.AnnonceTexteService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @Test
    void introuvable_rend404EnTexteSimpleSansDetail() {
        ResponseEntity<String> reponse = controller.introuvable(new NoSuchElementException("Annonce 9 chez RENEE"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reponse.getBody()).isEqualTo("Contenu introuvable");
        assertThat(reponse.getHeaders().getContentType().toString()).startsWith("text/plain");
    }
}
