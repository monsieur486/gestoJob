package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import com.mr486.gestojob.service.EntrepriseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomePageController {

    private final EntrepriseService entrepriseService;
    private final AnnonceService annonceService;

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


    private String getEntreprisesMessage(int nbr) {
        return nbr + " entreprise" + (nbr > 1 ? "s" : "");
    }

    private String getAnnoncesMessage(int nbr) {
        return nbr + " annonce" + (nbr > 1 ? "s" : "");
    }
}
