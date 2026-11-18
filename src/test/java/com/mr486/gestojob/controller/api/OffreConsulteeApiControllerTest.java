package com.mr486.gestojob.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.api.OffreConsulteeDemande;
import com.mr486.gestojob.dto.api.OffreConsulteeReponse;
import com.mr486.gestojob.service.OffreConsulteeService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Vérifie que l'endpoint d'API délègue au service et répond 201.
 */
class OffreConsulteeApiControllerTest {

    private final OffreConsulteeService service = mock(OffreConsulteeService.class);
    private final OffreConsulteeApiController controleur = new OffreConsulteeApiController(service);

    @Test
    void creer_repond201AvecLaReponse() {
        OffreConsulteeDemande demande = new OffreConsulteeDemande("Dév", "Acme", null, "POSTE", null, null);
        OffreConsulteeReponse reponse = new OffreConsulteeReponse(3L, LocalDate.of(2026, 11, 18), "POSTE");
        when(service.ajouterDepuisApi(demande)).thenReturn(reponse);

        ResponseEntity<OffreConsulteeReponse> resultat = controleur.creer(demande);

        assertThat(resultat.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resultat.getBody()).isSameAs(reponse);
    }
}
