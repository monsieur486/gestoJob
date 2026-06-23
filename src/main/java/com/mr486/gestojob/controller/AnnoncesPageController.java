package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.RechercheAnnonceForm;
import com.mr486.gestojob.service.AnnonceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur MVC gérant les pages liées aux annonces : affichage paginé,
 * recherche, et mise à jour du statut (refus ou acceptation) d'une annonce.
 */
@Controller
@RequiredArgsConstructor
public class AnnoncesPageController {

    private final AnnonceService annonceService;


    /**
     * Affiche la liste paginée des annonces, avec recherche optionnelle et
     * possibilité d'inclure les annonces archivées.
     *
     * <p><b>Exemple :</b> GET /annonces?page=2&amp;q=java&amp;archives=true charge la page 2 (index 1) des annonces correspondant à « java », archives incluses, et retourne la vue {@code annonces}.</p>
     *
     * @param model    le modèle Thymeleaf alimenté pour la vue
     * @param page     le numéro de page demandé (commence à 1)
     * @param q        le terme de recherche optionnel
     * @param archives indique si les annonces archivées doivent être incluses
     * @return le nom de la vue Thymeleaf {@code annonces}
     */
    @GetMapping("/annonces")
    public String annoncesView(
            Model model,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "archives", defaultValue = "false") boolean archives
    ) {
        model.addAttribute("page_active", "annonces");

        int pageIndex = Math.max(0, page - 1);

        RechercheAnnonceForm form = RechercheAnnonceForm.builder()
                .recherche(q)
                .avecArchives(archives)
                .build();
        model.addAttribute("searchForm", form);

        var pageResult = annonceService.searchAnnoncesPage(form, pageIndex);

        model.addAttribute("annoncesPage", pageResult);
        model.addAttribute("annonces", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("searchQuery", q);
        model.addAttribute("includeArchives", archives);

        return "annonces";
    }

    /**
     * Marque une annonce comme refusée (réponse négative).
     *
     * <p><b>Exemple :</b> POST /annonces/7/negatif marque l'annonce 7 « refusée » via setRefus(7) puis redirige vers /annonces.</p>
     *
     * @param id l'identifiant de l'annonce concernée
     * @return une redirection vers la page {@code /annonces}
     */
    @PostMapping("/annonces/{id}/negatif")
    public String postReponse(@PathVariable Long id) {
        annonceService.setRefus(id);
        return "redirect:/annonces";
    }

    /**
     * Marque une annonce comme acceptée (réponse positive).
     *
     * <p><b>Exemple :</b> POST /annonces/7/positif marque l'annonce 7 « acceptée » via setAccepte(7) puis redirige vers /annonces.</p>
     *
     * @param id l'identifiant de l'annonce concernée
     * @return une redirection vers la page {@code /annonces}
     */
    @PostMapping("/annonces/{id}/positif")
    public String postReponseAccepte(@PathVariable Long id) {
        annonceService.setAccepte(id);
        return "redirect:/annonces";
    }

    /**
     * Supprime définitivement une annonce depuis la liste des annonces.
     *
     * <p><b>Exemple :</b> POST /annonces/7/supprimer supprime l'annonce 7 via deleteAnnonce(7) puis redirige vers /annonces.</p>
     *
     * @param id l'identifiant de l'annonce à supprimer
     * @return une redirection vers la page {@code /annonces}
     */
    @PostMapping("/annonces/{id}/supprimer")
    public String supprimerAnnonce(@PathVariable Long id) {
        annonceService.deleteAnnonce(id);
        return "redirect:/annonces";
    }
}
