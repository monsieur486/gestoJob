package com.mr486.gestojob.controller.api;

import com.mr486.gestojob.dto.api.ErreurApi;
import com.mr486.gestojob.exception.MetierException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit en erreurs JSON les exceptions des points d'entrée de l'API.
 *
 * <p>Limité au paquet du {@link CandidatureApiController} et prioritaire, il passe avant le
 * conseil global des pages ({@code ErreurConseil}), qui rendrait une page HTML. Les messages ne
 * reprennent jamais le contenu de la requête (lettre, poste). La méthode HTTP non prévue (405),
 * refusée avant le choix du contrôleur, est traitée à part par {@link ApiMethodeConseil}.</p>
 *
 * <p><b>Exemple :</b> une règle métier violée répond {@code 400}
 * {@code {"erreur":"La ville est obligatoire."}}.</p>
 */
@Slf4j
@RestControllerAdvice(basePackageClasses = CandidatureApiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiErreurConseil {

    /**
     * Rend une règle métier violée en 400.
     *
     * <p><b>Exemple :</b> {@code MetierException("La ville est obligatoire.")} donne 400.</p>
     *
     * @param exception la violation, dont le message est destiné à l'appelant
     * @return 400 et le message
     */
    @ExceptionHandler(MetierException.class)
    public ResponseEntity<ErreurApi> metier(MetierException exception) {
        return ResponseEntity.badRequest().body(new ErreurApi(exception.getMessage()));
    }

    /**
     * Rend un échec de validation du corps en 400, un message par champ fautif.
     *
     * <p><b>Exemple :</b> {@code entreprise : ne doit pas être nul ; lettreHtml : ne doit pas être vide}.</p>
     *
     * @param exception l'échec de validation
     * @return 400 et les messages joints par « ; »
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurApi> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(ApiErreurConseil::decrire)
                .collect(Collectors.joining(" ; "));
        return ResponseEntity.badRequest().body(new ErreurApi(message));
    }

    /**
     * Rend un corps JSON illisible en 400, sans détail technique.
     *
     * <p><b>Exemple :</b> le corps « pas du json » donne 400.</p>
     *
     * @param exception l'erreur de lecture (son message n'est pas exposé)
     * @return 400 et un message fixe
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurApi> jsonIllisible(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ErreurApi("Corps de requête JSON illisible."));
    }

    /**
     * Rend une ressource inconnue (ou à l'autre titulaire) en 404, sans détail.
     *
     * <p><b>Exemple :</b> une entreprise d'id inconnu donne 404 {@code "Ressource introuvable."}.</p>
     *
     * @param exception l'absence constatée (son message n'est pas exposé)
     * @return 404 et un message fixe
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErreurApi> introuvable(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErreurApi("Ressource introuvable."));
    }

    /**
     * Rend un type de contenu non géré en 415, sans détail.
     *
     * <p><b>Exemple :</b> un corps {@code text/plain} sur {@code POST /api/candidatures} donne 415.</p>
     *
     * @param exception le type refusé (son message n'est pas exposé)
     * @return 415 et un message fixe
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErreurApi> typeNonSupporte(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErreurApi("Type de contenu non pris en charge."));
    }

    /**
     * Rend toute erreur inattendue en 500 JSON, sans détail ; seuls le message et la classe de
     * l'exception sont journalisés, jamais le corps de la requête.
     *
     * <p><b>Exemple :</b> une {@code IllegalStateException} interne donne 500 {@code "Erreur interne."}.</p>
     *
     * @param exception l'erreur inattendue
     * @return 500 et un message fixe
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurApi> erreurInterne(Exception exception) {
        log.error("erreur interne de l'API : {} ({})", exception.getMessage(), exception.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErreurApi("Erreur interne."));
    }

    // « champ : message » ; jamais la valeur rejetée, qui pourrait être le contenu d'une lettre.
    private static String decrire(FieldError erreur) {
        return erreur.getField() + " : " + erreur.getDefaultMessage();
    }
}
