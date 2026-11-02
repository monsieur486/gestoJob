package com.mr486.gestojob.controller;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Transforme en page 404 les ressources métier introuvables.
 *
 * <p>Un id inexistant et un id appartenant à l'autre titulaire lèvent la même
 * {@link NoSuchElementException} : la réponse est volontairement identique et sans détail,
 * pour ne rien révéler de l'existence des données de l'autre titulaire.</p>
 */
@ControllerAdvice
public class ErreurConseil {

    /**
     * Rend la page {@code erreur-404} avec le statut HTTP 404.
     *
     * <p><b>Exemple :</b> GET /entreprises/999 (inconnue, ou à l'autre titulaire) rend la page
     * « Page introuvable » en 404, sans le message de l'exception.</p>
     *
     * @param exception l'exception levée par le service (son message n'est pas exposé)
     * @return la vue d'erreur 404, sans donnée dans le modèle
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ModelAndView introuvable(NoSuchElementException exception) {
        ModelAndView vue = new ModelAndView("erreur-404");
        vue.setStatus(HttpStatus.NOT_FOUND);
        return vue;
    }
}
