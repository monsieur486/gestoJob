package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import com.mr486.gestojob.service.EntrepriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur MVC de la page d'accueil publique : affiche les statistiques
 * (nombre d'entreprises et d'annonces) ainsi que la liste paginée des annonces
 * ayant reçu une réponse positive.
 */
@Controller
@RequiredArgsConstructor
public class HomePageController {

    private final EntrepriseService entrepriseService;
    private final AnnonceService annonceService;

    /**
     * Affiche la page d'accueil avec les compteurs et les annonces positives
     * paginées.
     *
     * <p><b>Exemple :</b> GET /?page=2 charge la page 2 (index 1) des annonces positives, ajoute les compteurs « 3 entreprises » et « 12 annonces » au modèle, et retourne la vue {@code accueil}.</p>
     *
     * @param model le modèle Thymeleaf alimenté pour la vue
     * @param page  le numéro de page demandé (commence à 1)
     * @return le nom de la vue Thymeleaf {@code accueil}
     */
    @GetMapping("/")
    public String publicView(Model model, @RequestParam(name = "page", defaultValue = "1") int page) {
        int pageIndex = Math.max(0, page - 1);

        var pageResult = annonceService.getAllPositifListePage(pageIndex);

        model.addAttribute("page_active", "home");
        model.addAttribute("nbrEntreprises", getEntreprisesMessage(entrepriseService.countAllEntreprises()));
        model.addAttribute("nbrAnnonces", getAnnoncesMessage((int) annonceService.countAnnonces()));
        model.addAttribute("annoncesPage", pageResult);
        model.addAttribute("annonces", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        return "accueil";
    }


    // Construit le libellé du nombre d'entreprises en gérant le pluriel.
    // Exemple : getEntreprisesMessage(1) renvoie "1 entreprise" ; getEntreprisesMessage(3) renvoie "3 entreprises".
    private String getEntreprisesMessage(int nbr) {
        return nbr + " entreprise" + (nbr > 1 ? "s" : "");
    }

    // Construit le libellé du nombre d'annonces en gérant le pluriel.
    // Exemple : getAnnoncesMessage(1) renvoie "1 annonce" ; getAnnoncesMessage(12) renvoie "12 annonces".
    private String getAnnoncesMessage(int nbr) {
        return nbr + " annonce" + (nbr > 1 ? "s" : "");
    }
}
