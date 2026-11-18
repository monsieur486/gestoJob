package com.mr486.gestojob.controller.api;

import com.mr486.gestojob.dto.api.ErreurApi;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Rend en JSON, pour l'API seulement, une méthode HTTP non prévue (405).
 *
 * <p>Ce cas ne peut pas vivre dans {@link ApiErreurConseil} : Spring refuse la méthode
 * <em>avant</em> d'avoir choisi un contrôleur, et un conseil limité à un paquet n'est alors pas
 * consulté. Ce conseil est donc global, mais il ne répond que pour les URL {@code /api/} ;
 * ailleurs il relance l'exception, que Spring traite comme avant (405 sans corps pour les pages).</p>
 *
 * <p><b>Exemple :</b> {@code DELETE /api/candidatures} donne 405
 * {@code {"erreur":"Méthode non autorisée."}} avec l'en-tête {@code Allow: POST} ;
 * {@code DELETE /entreprises/ajout} reste un 405 standard.</p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiMethodeConseil {

    private static final String PREFIXE_API = "/api/";

    /**
     * Rend une méthode non prévue sur {@code /api/**} en 405 JSON ; relance l'exception ailleurs.
     *
     * <p><b>Exemple :</b> {@code PUT /api/modele-lettre} donne 405 et {@code Allow: GET}.</p>
     *
     * @param exception la méthode refusée (son message n'est pas exposé)
     * @param requete   la requête, dont le chemin décide du traitement
     * @return 405, l'en-tête {@code Allow} et un message fixe
     * @throws HttpRequestMethodNotSupportedException hors API, pour laisser Spring répondre
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErreurApi> methodeNonAutorisee(HttpRequestMethodNotSupportedException exception,
                                                         HttpServletRequest requete)
            throws HttpRequestMethodNotSupportedException {
        String chemin = requete.getRequestURI().substring(requete.getContextPath().length());
        if (!chemin.startsWith(PREFIXE_API)) {
            throw exception;
        }
        HttpHeaders entetes = new HttpHeaders();
        if (exception.getSupportedHttpMethods() != null) {
            entetes.setAllow(exception.getSupportedHttpMethods());
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(entetes)
                .body(new ErreurApi("Méthode non autorisée."));
    }
}
