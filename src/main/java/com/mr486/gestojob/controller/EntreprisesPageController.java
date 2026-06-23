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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur MVC gérant les pages liées aux entreprises : liste paginée,
 * recherche, activation/désactivation, ajout, fiche détaillée, ainsi que
 * l'ajout et la suppression d'annonces rattachées à une entreprise.
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
     * <p><b>Exemple :</b> GET /entreprises?active=true liste les entreprises actives (sans pagination) ; GET /entreprises?q=acme liste les entreprises dont le nom contient « acme » ; GET /entreprises?page=2 retourne la page 2 (index 1) de la liste complète paginée. Toujours la vue {@code entreprises}.</p>
     *
     * @param model  le modèle Thymeleaf alimenté pour la vue
     * @param page   le numéro de page demandé (commence à 1)
     * @param q      le terme de recherche optionnel sur le nom
     * @param active indique si seules les entreprises actives doivent être affichées
     * @return le nom de la vue Thymeleaf {@code entreprises}
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

        int pageIndex = Math.max(0, page - 1);

        if (active) {
            var pageResult = entrepriseService.rechercheEntrepriseActivePage(pageIndex);
            model.addAttribute("entreprises", pageResult.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pageResult.getTotalPages());
            return "entreprises";
        }

        if (StringUtils.hasText(q)) {
            var pageResult = entrepriseService.rechercheEntrepriseParNomPage(q, pageIndex);
            model.addAttribute("entreprises", pageResult.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pageResult.getTotalPages());
            return "entreprises";
        }

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
     * <p><b>Exemple :</b> POST /entreprises/3/activate?page=2 active l'entreprise 3 via activeEntreprise(3) puis redirige vers /entreprises?page=2.</p>
     *
     * @param id   l'identifiant de l'entreprise à activer
     * @param page le numéro de page vers lequel rediriger
     * @return une redirection vers la page {@code /entreprises} demandée
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
     * <p><b>Exemple :</b> POST /entreprises/3/desactivate?page=2 désactive l'entreprise 3 via desactiveEntreprise(3) puis redirige vers /entreprises?page=2.</p>
     *
     * @param id   l'identifiant de l'entreprise à désactiver
     * @param page le numéro de page vers lequel rediriger
     * @return une redirection vers la page {@code /entreprises} demandée
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
     * <p><b>Exemple :</b> GET /entreprises/ajout place un EntrepriseForm vide dans le modèle et retourne la vue {@code add-entreprise}.</p>
     *
     * @param model le modèle Thymeleaf alimenté avec un formulaire vierge
     * @return le nom de la vue Thymeleaf {@code add-entreprise}
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
     * <p><b>Exemple :</b> POST /entreprises/ajout avec un formulaire valide enregistre l'entreprise via save(form) qui retourne l'id 12, puis redirige vers /entreprises/12 ; si le formulaire est invalide (ou si save lève une RuntimeException), retourne la vue {@code add-entreprise}.</p>
     *
     * @param form          le formulaire d'entreprise validé
     * @param bindingResult le résultat de la validation du formulaire
     * @param model         le modèle Thymeleaf alimenté pour la vue
     * @return la vue {@code add-entreprise} en cas d'erreur, ou une redirection
     *         vers la fiche de l'entreprise créée en cas de succès
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
     * <p><b>Exemple :</b> GET /entreprises/3 charge l'entreprise 3, ses contacts et ses annonces, ajoute des formulaires vierges (ContactForm, AnnonceForm) et retourne la vue {@code entreprise_detail}.</p>
     *
     * @param id    l'identifiant de l'entreprise à afficher
     * @param model le modèle Thymeleaf alimenté pour la vue
     * @return le nom de la vue Thymeleaf {@code entreprise_detail}
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
     * <p><b>Exemple :</b> POST /entreprises/3/annonces/ajout fixe entrepriseId=3 sur le formulaire ; si valide, enregistre l'annonce via saveForm(form) puis redirige vers /entreprises/3 ; si invalide (ou si saveForm lève une RuntimeException), recharge entreprise/contacts/annonces et retourne la vue {@code entreprise_detail}.</p>
     *
     * @param id            l'identifiant de l'entreprise concernée
     * @param form          le formulaire d'annonce validé
     * @param bindingResult le résultat de la validation du formulaire
     * @param model         le modèle Thymeleaf alimenté pour la vue
     * @return la vue {@code entreprise_detail} en cas d'erreur, ou une
     *         redirection vers la fiche de l'entreprise en cas de succès
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

    /**
     * Supprime une annonce rattachée à une entreprise puis redirige vers la fiche
     * de cette entreprise. En cas d'échec, le message est transmis en flash.
     *
     * <p><b>Exemple :</b> POST /entreprises/3/annonces/7/supprimer supprime l'annonce 7 via deleteAnnonce(7) puis redirige vers /entreprises/3 ; en cas d'exception, redirige vers /entreprises/3 avec un message d'erreur (flash).</p>
     *
     * @param entrepriseId       l'identifiant de l'entreprise (cible de la redirection)
     * @param annonceId          l'identifiant de l'annonce à supprimer
     * @param redirectAttributes les attributs flash transmis à la redirection
     * @return une redirection vers la fiche de l'entreprise
     */
    @PostMapping("/entreprises/{entrepriseId}/annonces/{annonceId}/supprimer")
    public String supprimerAnnonce(
            @PathVariable Integer entrepriseId,
            @PathVariable Long annonceId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            annonceService.deleteAnnonce(annonceId);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/entreprises/" + entrepriseId;
    }
}