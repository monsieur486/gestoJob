package com.mr486.gestojob.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur MVC gérant la page des paramètres de l'application.
 */
@Controller
@RequiredArgsConstructor
public class ParametresPageController {

    /**
     * Affiche la page des paramètres.
     *
     * <p><b>Exemple :</b> GET /parametres affiche la vue parametres avec l'onglet Paramètres actif.</p>
     *
     * @param model le modèle Thymeleaf alimenté pour la vue
     * @return le nom de la vue Thymeleaf {@code parametres}
     */
    @GetMapping("/parametres")
    public String parametresView(Model model) {
        model.addAttribute("page_active", "parametres");
        return "parametres";
    }
}
