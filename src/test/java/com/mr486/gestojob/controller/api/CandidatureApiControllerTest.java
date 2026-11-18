package com.mr486.gestojob.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.api.CandidatureDemande;
import com.mr486.gestojob.dto.api.CandidatureReponse;
import com.mr486.gestojob.dto.api.EntrepriseApi;
import com.mr486.gestojob.dto.api.ModeleLettreApi;
import com.mr486.gestojob.service.CandidatureApiService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CandidatureApiControllerTest {

    private final CandidatureApiService service = mock(CandidatureApiService.class);
    private final CandidatureApiController controleur = new CandidatureApiController(service);

    @Test
    void modeleLettre_delegueAuService() {
        ModeleLettreApi modele = mock(ModeleLettreApi.class);
        when(service.modeleLettre()).thenReturn(modele);

        assertThat(controleur.modeleLettre()).isSameAs(modele);
    }

    @Test
    void entreprises_delegueAuServiceAvecLeNom() {
        List<EntrepriseApi> resultat = List.of(mock(EntrepriseApi.class));
        when(service.rechercherEntreprises("acme")).thenReturn(resultat);

        assertThat(controleur.entreprises("acme")).isSameAs(resultat);
        verify(service).rechercherEntreprises("acme");
    }

    @Test
    void creer_renvoie201EtLeCorpsDuService() {
        CandidatureDemande demande = mock(CandidatureDemande.class);
        CandidatureReponse reponse = new CandidatureReponse(5L, 31, 77L, 1, true, false, true);
        when(service.creer(demande)).thenReturn(reponse);

        ResponseEntity<CandidatureReponse> resultat = controleur.creer(demande);

        assertThat(resultat.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resultat.getBody()).isSameAs(reponse);
    }
}
