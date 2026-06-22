package com.mr486.gestojob.controller;


import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Contrôleur MVC gérant l'ajout d'un contact rattaché à une entreprise :
 * affichage du formulaire et traitement de sa soumission.
 */
@Controller
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * Affiche le formulaire d'ajout d'un contact pour une entreprise.
     *
     * <p><b>Exemple :</b> GET /entreprises/3/contacts/ajout place un ContactForm vide dans le modèle et retourne la vue {@code add-contact}.</p>
     *
     * @param model le modèle Thymeleaf alimenté avec un formulaire vierge
     * @return le nom de la vue Thymeleaf {@code add-contact}
     */
    @GetMapping("/entreprises/{id}/contacts/ajout")
    public String ajoutEntrepriseView(Model model) {
        model.addAttribute("page_active", "entreprises");
        model.addAttribute("form", new ContactForm());
        return "add-contact";
    }

    /**
     * Traite la soumission du formulaire d'ajout d'un contact. En cas d'erreur
     * de validation ou d'exception métier, réaffiche le formulaire avec le
     * message d'erreur ; sinon, enregistre le contact et redirige vers la fiche
     * de l'entreprise.
     *
     * <p><b>Exemple :</b> POST /entreprises/3/contacts/ajout avec un formulaire valide enregistre le contact via saveContact(form, 3) puis redirige vers /entreprises/3 ; si le formulaire est invalide (ou si saveContact lève une RuntimeException), retourne la vue {@code entreprises/3/contacts/ajout}.</p>
     *
     * @param form          le formulaire de contact validé
     * @param id            l'identifiant de l'entreprise concernée
     * @param bindingResult le résultat de la validation du formulaire
     * @param model         le modèle Thymeleaf alimenté pour la vue
     * @return le nom de la vue du formulaire en cas d'erreur, ou une redirection
     *         vers la fiche de l'entreprise en cas de succès
     */
    @PostMapping("/entreprises/{id}/contacts/ajout")
    public String ajoutEntrepriseSubmit(
            @Valid @ModelAttribute("form") ContactForm form,
            @PathVariable Integer id,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "entreprises/" + id + "/contacts/ajout";
        }

        try {
            contactService.saveContact(form, id);
            return "redirect:/entreprises/" + id;
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "entreprises/" + id + "/contacts/ajout";
        }
    }

}
