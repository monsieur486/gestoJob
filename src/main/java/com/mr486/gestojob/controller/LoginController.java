package com.mr486.gestojob.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Affiche la page de connexion. Le traitement de l'authentification (POST)
 * est pris en charge par Spring Security.
 */
@Controller
public class LoginController {

    /**
     * Affiche le formulaire de connexion.
     *
     * @return le nom de la vue de connexion
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
