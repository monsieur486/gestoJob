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

/**
 * Contrôleur MVC gérant les pages liées aux entreprises : liste paginée,
 * recherche, activation/désactivation, ajout, fiche détaillée, ainsi que
 * l'ajout d'annonces rattachées à une entreprise.
 */
@Controller
@RequiredArgsConstructor
public class EntreprisesPageController {

    private final EntrepriseService entrepriseService;
    private final ContactService contactService;
    private final AnnonceService annonceService;

    /**
     * Affiche la liste des entreprises. Selon les paramètres : uniquement les
     * entreprises actives, le résultat d'une recherche par nom, ou la liste
     * complète paginée.
     *
     * @param model  le modèle Thymeleaf alimenté pour la vue
     * @param page   le numéro de page demandé (commence à 1)
     * @param q      le terme de recherche optionnel sur le nom
     * @param active indique si seules les entreprises actives doivent être affichées
     * @return le nom de la vue Thymeleaf {@code entreprises}
     *
     * <p><b>Exemple :</b> GET /entreprises?active=true liste les entreprises actives (sans pagination) ; GET /entreprises?q=acme liste les entreprises dont le nom contient « acme » ; GET /entreprises?page=2 retourne la page 2 (index 1) de la liste complète paginée. Toujours la vue {@code entreprises}.</p>
     */
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

    /**
     * Active une entreprise puis redirige vers la liste paginée.
     *
     * @param id   l'identifiant de l'entreprise à activer
     * @param page le numéro de page vers lequel rediriger
     * @return une redirection vers la page {@code /entreprises} demandée
     *
     * <p><b>Exemple :</b> POST /entreprises/3/activate?page=2 active l'entreprise 3 via activeEntreprise(3) puis redirige vers /entreprises?page=2.</p>
     */
    @PostMapping("/entreprises/{id}/activate")
    public String activateEntreprise(
            @PathVariable Integer id,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        entrepriseService.activeEntreprise(id);
        return "redirect:/entreprises?page=" + page;
    }

    /**
     * Désactive une entreprise puis redirige vers la liste paginée.
     *
     * @param id   l'identifiant de l'entreprise à désactiver
     * @param page le numéro de page vers lequel rediriger
     * @return une redirection vers la page {@code /entreprises} demandée
     *
     * <p><b>Exemple :</b> POST /entreprises/3/desactivate?page=2 désactive l'entreprise 3 via desactiveEntreprise(3) puis redirige vers /entreprises?page=2.</p>
     */
    @PostMapping("/entreprises/{id}/desactivate")
    public String desactivateEntreprise(
            @PathVariable Integer id,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        entrepriseService.desactiveEntreprise(id);
        return "redirect:/entreprises?page=" + page;
    }

    /**
     * Affiche le formulaire d'ajout d'une entreprise.
     *
     * @param model le modèle Thymeleaf alimenté avec un formulaire vierge
     * @return le nom de la vue Thymeleaf {@code add-entreprise}
     *
     * <p><b>Exemple :</b> GET /entreprises/ajout place un EntrepriseForm vide dans le modèle et retourne la vue {@code add-entreprise}.</p>
     */
    @GetMapping("/entreprises/ajout")
    public String ajoutEntrepriseView(Model model) {
        model.addAttribute("page_active", "entreprises");
        model.addAttribute("form", new EntrepriseForm());
        return "add-entreprise";
    }

    /**
     * Traite la soumission du formulaire d'ajout d'une entreprise. En cas
     * d'erreur de validation ou d'exception métier, réaffiche le formulaire
     * avec le message d'erreur ; sinon, enregistre l'entreprise et redirige
     * vers sa fiche.
     *
     * @param form          le formulaire d'entreprise validé
     * @param bindingResult le résultat de la validation du formulaire
     * @param model         le modèle Thymeleaf alimenté pour la vue
     * @return la vue {@code add-entreprise} en cas d'erreur, ou une redirection
     *         vers la fiche de l'entreprise créée en cas de succès
     *
     * <p><b>Exemple :</b> POST /entreprises/ajout avec un formulaire valide enregistre l'entreprise via save(form) qui retourne l'id 12, puis redirige vers /entreprises/12 ; si le formulaire est invalide (ou si save lève une RuntimeException), retourne la vue {@code add-entreprise}.</p>
     */
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

    /**
     * Affiche la fiche détaillée d'une entreprise avec ses contacts, ses
     * annonces et les formulaires d'ajout associés.
     *
     * @param id    l'identifiant de l'entreprise à afficher
     * @param model le modèle Thymeleaf alimenté pour la vue
     * @return le nom de la vue Thymeleaf {@code entreprise_detail}
     *
     * <p><b>Exemple :</b> GET /entreprises/3 charge l'entreprise 3, ses contacts et ses annonces, ajoute des formulaires vierges (ContactForm, AnnonceForm) et retourne la vue {@code entreprise_detail}.</p>
     */
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

    /**
     * Traite la soumission du formulaire d'ajout d'une annonce rattachée à une
     * entreprise. En cas d'erreur de validation ou d'exception métier, réaffiche
     * la fiche de l'entreprise avec le message d'erreur ; sinon, enregistre
     * l'annonce et redirige vers la fiche.
     *
     * @param id            l'identifiant de l'entreprise concernée
     * @param form          le formulaire d'annonce validé
     * @param bindingResult le résultat de la validation du formulaire
     * @param model         le modèle Thymeleaf alimenté pour la vue
     * @return la vue {@code entreprise_detail} en cas d'erreur, ou une
     *         redirection vers la fiche de l'entreprise en cas de succès
     *
     * <p><b>Exemple :</b> POST /entreprises/3/annonces/ajout fixe entrepriseId=3 sur le formulaire ; si valide, enregistre l'annonce via saveForm(form) puis redirige vers /entreprises/3 ; si invalide (ou si saveForm lève une RuntimeException), recharge entreprise/contacts/annonces et retourne la vue {@code entreprise_detail}.</p>
     */
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