package com.mr486.gestojob.service;

import static org.mockito.Mockito.verify;

import com.mr486.gestojob.persistance.AnnonceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnonceStatutServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;

    @InjectMocks
    private AnnonceStatutService annonceStatutService;

    @Test
    void setDepasse_metLeStatutA3() {
        annonceStatutService.setDepasse(7L);
        verify(annonceRepository).updateStatusAnnonce(7L, 3);
    }

    @Test
    void setRefus_metLeStatutA4() {
        annonceStatutService.setRefus(7L);
        verify(annonceRepository).updateStatusAnnonce(7L, 4);
    }

    @Test
    void setAccepte_metLeStatutA5() {
        annonceStatutService.setAccepte(7L);
        verify(annonceRepository).updateStatusAnnonce(7L, 5);
    }
}
