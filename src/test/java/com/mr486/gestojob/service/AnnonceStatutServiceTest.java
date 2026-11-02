package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnonceStatutServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;
    @Mock
    private UtilisateurCourant utilisateurCourant;

    @InjectMocks
    private AnnonceStatutService annonceStatutService;

    @BeforeEach
    void setUp() {
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
        when(annonceRepository.updateStatusAnnonce(Titulaire.RENEE, 7L, 3)).thenReturn(1);
        when(annonceRepository.updateStatusAnnonce(Titulaire.RENEE, 7L, 4)).thenReturn(1);
        when(annonceRepository.updateStatusAnnonce(Titulaire.RENEE, 7L, 5)).thenReturn(1);
    }

    @Test
    void setDepasse_metLeStatutA3() {
        annonceStatutService.setDepasse(7L);
        verify(annonceRepository).updateStatusAnnonce(Titulaire.RENEE, 7L, 3);
    }

    @Test
    void setRefus_metLeStatutA4() {
        annonceStatutService.setRefus(7L);
        verify(annonceRepository).updateStatusAnnonce(Titulaire.RENEE, 7L, 4);
    }

    @Test
    void setAccepte_metLeStatutA5() {
        annonceStatutService.setAccepte(7L);
        verify(annonceRepository).updateStatusAnnonce(Titulaire.RENEE, 7L, 5);
    }

    @Test
    void setAccepte_idAutreTitulaire_leveExceptionEtNeModifieRien() {
        when(annonceRepository.updateStatusAnnonce(Titulaire.RENEE, 8L, 5)).thenReturn(0);

        assertThatThrownBy(() -> annonceStatutService.setAccepte(8L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void setRefus_idAutreTitulaire_leveException() {
        when(annonceRepository.updateStatusAnnonce(Titulaire.RENEE, 8L, 4)).thenReturn(0);

        assertThatThrownBy(() -> annonceStatutService.setRefus(8L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void setDepasse_idAutreTitulaire_leveException() {
        when(annonceRepository.updateStatusAnnonce(Titulaire.RENEE, 8L, 3)).thenReturn(0);

        assertThatThrownBy(() -> annonceStatutService.setDepasse(8L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
