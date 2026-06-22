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

@Controller
@RequiredArgsConstructor
public class AnnoncesPageController {

    private final AnnonceService annonceService;


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

    @PostMapping("/annonces/{id}/negatif")
    public String postReponse(@PathVariable Long id) {
        annonceService.setRefus(id);
        return "redirect:/annonces";
    }

    @PostMapping("/annonces/{id}/positif")
    public String postReponseAccepte(@PathVariable Long id) {
        annonceService.setAccepte(id);
        return "redirect:/annonces";
    }
}
