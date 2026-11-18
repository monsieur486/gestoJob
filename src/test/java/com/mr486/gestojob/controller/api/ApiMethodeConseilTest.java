package com.mr486.gestojob.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mr486.gestojob.dto.api.ErreurApi;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class ApiMethodeConseilTest {

    private final ApiMethodeConseil conseil = new ApiMethodeConseil();

    @Test
    void cheminApi_405JsonEtEnTeteAllow() throws HttpRequestMethodNotSupportedException {
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("DELETE", List.of("POST"));

        ResponseEntity<ErreurApi> reponse =
                conseil.methodeNonAutorisee(exception, new MockHttpServletRequest("DELETE", "/api/candidatures"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(reponse.getBody()).isEqualTo(new ErreurApi("Méthode non autorisée."));
        assertThat(reponse.getHeaders().getAllow()).containsExactly(HttpMethod.POST);
    }

    @Test
    void cheminApiAvecContexte_405Json() throws HttpRequestMethodNotSupportedException {
        MockHttpServletRequest requete = new MockHttpServletRequest("PUT", "/gestojob/api/modele-lettre");
        requete.setContextPath("/gestojob");

        ResponseEntity<ErreurApi> reponse =
                conseil.methodeNonAutorisee(new HttpRequestMethodNotSupportedException("PUT"), requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Hors API, l'exception est relancée telle quelle pour que Spring garde son traitement standard.
    @Test
    void cheminHorsApi_relanceLException() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE");

        assertThatThrownBy(() -> conseil.methodeNonAutorisee(
                exception, new MockHttpServletRequest("DELETE", "/entreprises/ajout")))
                .isSameAs(exception);
    }

    // « /apiculture » ne doit pas être pris pour l'API : le préfixe inclut la barre finale.
    @Test
    void cheminQuiCommenceParApiSansBarre_relanceLException() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE");

        assertThatThrownBy(() -> conseil.methodeNonAutorisee(
                exception, new MockHttpServletRequest("DELETE", "/apiculture")))
                .isSameAs(exception);
    }
}
