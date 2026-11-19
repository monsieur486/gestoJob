package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.OffreConsulteeForm;
import com.mr486.gestojob.model.MotifEcart;
import com.mr486.gestojob.model.OffreConsultee;
import com.mr486.gestojob.service.OffreConsulteeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Page du journal des offres consultées puis écartées : liste paginée pour tout
 * compte connecté (lecteur France Travail compris), saisie et suppression pour
 * le titulaire (POST réservés à {@code TITULAIRE} par {@code SecurityConfig}).
 */
@Controller
@RequiredArgsConstructor
public class ConsulteesPageController {

    private static final String VUE = "consultees";
    private static final String ATTRIBUT_FORM = "form";
    private static final String REDIRECTION = "redirect:/consultees";

    private final OffreConsulteeService offreConsulteeService;

    /**
     * Affiche une page du journal et le formulaire de saisie (date du jour pré-remplie).
     *
     * <p><b>Exemple :</b> {@code GET /consultees?page=2} affiche la deuxième page ;
     * {@code ?page=0} affiche la première, {@code ?page=99} la dernière existante.</p>
     *
     * @param model le modèle de la vue
     * @param page  le numéro de page (base 1)
     * @return la vue {@code consultees}
     */
    @GetMapping("/consultees")
    public String liste(Model model, @RequestParam(name = "page", defaultValue = "1") int page) {
        model.addAttribute("page_active", VUE);
        // Numéro hors bornes : ramené à la première (≤ 0) ou à la dernière page existante.
        int pageIndex = Math.max(0, page - 1);
        Page<OffreConsultee> resultat = offreConsulteeService.lister(pageIndex);
        if (resultat.getTotalPages() > 0 && pageIndex >= resultat.getTotalPages()) {
            pageIndex = resultat.getTotalPages() - 1;
            resultat = offreConsulteeService.lister(pageIndex);
        }
        model.addAttribute("consulteesPage", resultat);
        model.addAttribute("consultees", resultat.getContent());
        model.addAttribute("currentPage", pageIndex + 1);
        model.addAttribute("totalPages", resultat.getTotalPages());
        model.addAttribute("motifs", MotifEcart.values());
        if (!model.containsAttribute(ATTRIBUT_FORM)) {
            OffreConsulteeForm form = new OffreConsulteeForm();
            form.setDateConsultation(LocalDate.now());
            model.addAttribute(ATTRIBUT_FORM, form);
        }
        return VUE;
    }

    /**
     * Enregistre une offre consultée ; en cas d'erreur de saisie, réaffiche la
     * page avec les messages et la saisie conservée.
     *
     * <p><b>Exemple :</b> un lien {@code javascript:alert(1)} réaffiche la page avec
     * « Le lien doit commencer par http:// ou https:// ».</p>
     *
     * @param form          le formulaire soumis
     * @param bindingResult le résultat de la validation
     * @param model         le modèle de la vue
     * @return la redirection vers la liste, ou la vue en erreur
     */
    @PostMapping("/consultees")
    public String ajouter(@Valid @ModelAttribute(ATTRIBUT_FORM) OffreConsulteeForm form,
                          BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return liste(model, 1);
        }
        offreConsulteeService.ajouter(form);
        return REDIRECTION;
    }

    /**
     * Supprime une entrée du journal ; un id inconnu ou de l'autre titulaire donne une 404.
     *
     * <p><b>Exemple :</b> {@code POST /consultees/42/supprimer} puis retour à la liste.</p>
     *
     * @param id l'identifiant de l'entrée
     * @return la redirection vers la liste
     */
    @PostMapping("/consultees/{id}/supprimer")
    public String supprimer(@PathVariable Long id) {
        offreConsulteeService.supprimer(id);
        return REDIRECTION;
    }
}
