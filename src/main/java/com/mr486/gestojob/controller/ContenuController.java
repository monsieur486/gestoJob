package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import lombok.RequiredArgsConstructor;
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

    private final AnnonceService annonceService;

    /**
     * Renvoie le contenu textuel d'une annonce, au format texte brut UTF-8.
     *
     * @param id l'identifiant de l'annonce concernée
     * @return le contenu textuel de l'annonce
     *
     * <p><b>Exemple :</b> GET /contenu/7 retourne, en {@code text/plain; charset=UTF-8}, le texte renvoyé par getAnnonceTxtContenuById(7).</p>
     */
    @GetMapping(value = "/contenu/{id}", produces = "text/plain; charset=UTF-8")
    public String getContenuById(@PathVariable Long id) {
        return annonceService.getAnnonceTxtContenuById(id);
    }
}
