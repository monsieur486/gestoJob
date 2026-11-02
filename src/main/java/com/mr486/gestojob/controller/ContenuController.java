package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceTexteService;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST exposant le contenu textuel brut d'une annonce, destiné
 * notamment au copier-coller (réponse en {@code text/plain}).
 */
@RestController
@RequiredArgsConstructor
public class ContenuController {

    private final AnnonceTexteService annonceTexteService;

    /**
     * Renvoie le contenu textuel d'une annonce, au format texte brut UTF-8.
     *
     * <p><b>Exemple :</b> GET /contenu/7 retourne, en {@code text/plain; charset=UTF-8}, le texte renvoyé par getAnnonceTxtContenuById(7).</p>
     *
     * @param id l'identifiant de l'annonce concernée
     * @return le contenu textuel de l'annonce
     */
    @GetMapping(value = "/contenu/{id}", produces = "text/plain; charset=UTF-8")
    public String getContenuById(@PathVariable Long id) {
        return annonceTexteService.getAnnonceTxtContenuById(id);
    }

    /**
     * Répond 404 en texte brut quand l'annonce est inconnue ou appartient à l'autre titulaire
     * (même réponse dans les deux cas, sans détail).
     *
     * <p><b>Exemple :</b> GET /contenu/999 retourne le statut 404 et le corps « Contenu introuvable ».</p>
     *
     * @param exception l'exception levée par le service (son message n'est pas exposé)
     * @return une réponse 404 en {@code text/plain; charset=UTF-8}
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> introuvable(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .body("Contenu introuvable");
    }
}
