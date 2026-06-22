package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.service.AnnonceService;
import com.mr486.gestojob.service.ContactService;
import com.mr486.gestojob.service.EntrepriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class EntreprisesPageController {

    private final EntrepriseService entrepriseService;
    private final ContactService contactService;
    private final AnnonceService annonceService;

    @GetMapping("/entreprises")
    public String entreprisesView(
            Model model,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "active", defaultValue = "false") boolean active
    ) {
        model.addAttribute("page_active", "entreprises");
        model.addAttribute("searchQuery", q);
        model.addAttribute("activeOnly", active);

        if (active) {
            model.addAttribute("entreprises", entrepriseService.rechercheEntrepriseActive());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            return "entreprises";
        }

        if (StringUtils.hasText(q)) {
            model.addAttribute("entreprises", entrepriseService.rechercheEntrepriseParNom(q));
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 0);
            return "entreprises";
        }

        int pageIndex = Math.max(0, page - 1);
        var pageResult = entrepriseService.getAllListePage(pageIndex);

        model.addAttribute("entreprisesPage", pageResult);
        model.addAttribute("entreprises", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());

        return "entreprises";
    }

    @PostMapping("/entreprises/{id}/activate")
    public String activateEntreprise(
            @PathVariable Integer id,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        entrepriseService.activeEntreprise(id);
        return "redirect:/entreprises?page=" + page;
    }

    @PostMapping("/entreprises/{id}/desactivate")
    public String desactivateEntreprise(
            @PathVariable Integer id,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        entrepriseService.desactiveEntreprise(id);
        return "redirect:/entreprises?page=" + page;
    }

    @GetMapping("/entreprises/ajout")
    public String ajoutEntrepriseView(Model model) {
        model.addAttribute("page_active", "entreprises");
        model.addAttribute("form", new EntrepriseForm());
        return "add-entreprise";
    }

    @PostMapping("/entreprises/ajout")
    public String ajoutEntrepriseSubmit(
            @Valid @ModelAttribute("form") EntrepriseForm form,
            BindingResult bindingResult,
            Model model
    ) {
        model.addAttribute("page_active", "entreprises");

        if (bindingResult.hasErrors()) {
            return "add-entreprise";
        }

        try {
            int id=entrepriseService.save(form);
            return "redirect:/entreprises/"+id;
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "add-entreprise";
        }
    }

    @GetMapping("/entreprises/{id}")
    public String detailEntrepriseView(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "entreprises");
        model.addAttribute("entreprise", entrepriseService.getEntreprise(id));
        model.addAttribute("contacts", contactService.getAllContact(id));
        model.addAttribute("form", new ContactForm());
        model.addAttribute("annonces", annonceService.annoncesListeByEntrepriseId(id));
        model.addAttribute("annonceForm", new AnnonceForm());
        return "entreprise_detail";
    }

    @PostMapping("/entreprises/{id}/annonces/ajout")
    public String ajoutAnnonceSubmit(
            @PathVariable Integer id,
            @Valid @ModelAttribute("annonceForm") AnnonceForm form,
            BindingResult bindingResult,
            Model model
    ) {
        model.addAttribute("page_active", "entreprises");

        form.setEntrepriseId(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("entreprise", entrepriseService.getEntreprise(id));
            model.addAttribute("contacts", contactService.getAllContact(id));
            model.addAttribute("form", new ContactForm());
            model.addAttribute("annonces", annonceService.annoncesListeByEntrepriseId(id));
            return "entreprise_detail";
        }

        try {
            annonceService.saveForm(form);
            return "redirect:/entreprises/" + id;
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("entreprise", entrepriseService.getEntreprise(id));
            model.addAttribute("contacts", contactService.getAllContact(id));
            model.addAttribute("form", new ContactForm());
            model.addAttribute("annonces", annonceService.annoncesListeByEntrepriseId(id));
            return "entreprise_detail";
        }
    }
}