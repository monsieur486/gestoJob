package com.mr486.gestojob.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.mr486.gestojob.dto.api.ErreurApi;
import com.mr486.gestojob.exception.MetierException;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiErreurConseilTest {

    private final ApiErreurConseil conseil = new ApiErreurConseil();

    @Test
    void metier_400AvecLeMessage() {
        ResponseEntity<ErreurApi> reponse = conseil.metier(new MetierException("La ville est obligatoire."));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).isEqualTo(new ErreurApi("La ville est obligatoire."));
    }

    @Test
    void validation_400AvecLesMessagesJoints() throws NoSuchMethodException {
        BeanPropertyBindingResult resultat = new BeanPropertyBindingResult(new Object(), "demande");
        resultat.addError(new FieldError("demande", "entreprise", "ne doit pas être nul"));
        resultat.addError(new FieldError("demande", "lettreHtml", "ne doit pas être vide"));
        MethodParameter parametre = new MethodParameter(
                getClass().getDeclaredMethod("methodeFactice", String.class), 0);

        ResponseEntity<ErreurApi> reponse = conseil.validation(
                new MethodArgumentNotValidException(parametre, resultat));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody().erreur())
                .isEqualTo("entreprise : ne doit pas être nul ; lettreHtml : ne doit pas être vide");
    }

    @Test
    void jsonIllisible_400MessageFixe() {
        ResponseEntity<ErreurApi> reponse = conseil.jsonIllisible(
                new HttpMessageNotReadableException("détail interne", mock(HttpInputMessage.class)));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).isEqualTo(new ErreurApi("Corps de requête JSON illisible."));
    }

    @Test
    void introuvable_404MessageFixe() {
        ResponseEntity<ErreurApi> reponse = conseil.introuvable(new NoSuchElementException("id 9"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reponse.getBody()).isEqualTo(new ErreurApi("Ressource introuvable."));
    }

    @Test
    void methodeNonAutorisee_405MessageFixe() {
        ResponseEntity<ErreurApi> reponse = conseil.methodeNonAutorisee(
                new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(reponse.getBody()).isEqualTo(new ErreurApi("Méthode non autorisée."));
    }

    @Test
    void typeNonSupporte_415MessageFixe() {
        ResponseEntity<ErreurApi> reponse = conseil.typeNonSupporte(
                new HttpMediaTypeNotSupportedException("text/plain"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(reponse.getBody()).isEqualTo(new ErreurApi("Type de contenu non pris en charge."));
    }

    @Test
    void erreurInattendue_500MessageFixeSansDetail() {
        ResponseEntity<ErreurApi> reponse = conseil.erreurInterne(new IllegalStateException("secret interne"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(reponse.getBody()).isEqualTo(new ErreurApi("Erreur interne."));
    }

    // Sert uniquement à fournir un MethodParameter à l'exception de validation.
    @SuppressWarnings("unused")
    private void methodeFactice(String valeur) {
        // volontairement vide
    }
}
