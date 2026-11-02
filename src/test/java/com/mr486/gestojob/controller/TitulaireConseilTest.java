package com.mr486.gestojob.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.securite.UtilisateurCourant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TitulaireConseilTest {

    @Mock
    private UtilisateurCourant utilisateurCourant;

    @InjectMocks
    private TitulaireConseil conseil;

    @Test
    void titulaireAffichage_retourneLeLibelleDuTitulaire() {
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.LAURENT);

        assertThat(conseil.titulaireAffichage()).isEqualTo(Titulaire.LAURENT.getAffichage());
    }

    @Test
    void titulaireAffichage_retourneNull_sansUtilisateurAuthentifie() {
        when(utilisateurCourant.titulaire()).thenThrow(new IllegalStateException("anonyme"));

        assertThat(conseil.titulaireAffichage()).isNull();
    }
}
