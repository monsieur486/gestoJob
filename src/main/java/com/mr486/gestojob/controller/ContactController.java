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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String ajoutContactView(Model model) {
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
     * <p><b>Exemple :</b> POST /entreprises/3/contacts/ajout avec un formulaire valide enregistre le contact via saveContact(form, 3) puis redirige vers /entreprises/3 ; si le formulaire est invalide ou si saveContact lève une RuntimeException (ex. email en doublon), redirige vers /entreprises/3 avec un message d'erreur (flash).</p>
     *
     * @param form               le formulaire de contact validé
     * @param id                 l'identifiant de l'entreprise concernée
     * @param bindingResult      le résultat de la validation du formulaire
     * @param redirectAttributes les attributs flash transmis à la redirection
     * @return une redirection vers la fiche de l'entreprise (avec message d'erreur
     *         en cas d'échec, contact enregistré en cas de succès)
     */
    @PostMapping("/entreprises/{id}/contacts/ajout")
    public String ajoutContactSubmit(
            @Valid @ModelAttribute("form") ContactForm form,
            @PathVariable Integer id,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getFieldError() != null
                            ? bindingResult.getFieldError().getDefaultMessage()
                            : "Formulaire de contact invalide.");
            return "redirect:/entreprises/" + id;
        }

        try {
            contactService.saveContact(form, id);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/entreprises/" + id;
    }

    /**
     * Supprime un contact puis redirige vers la fiche de son entreprise. En cas
     * d'échec (contact encore rattaché à des annonces), le message est transmis
     * en flash.
     *
     * <p><b>Exemple :</b> POST /entreprises/3/contacts/9/supprimer supprime le contact 9 via deleteContact(9) puis redirige vers /entreprises/3 ; si le contact est rattaché à des annonces, redirige avec un message d'erreur.</p>
     *
     * @param entrepriseId       l'identifiant de l'entreprise (cible de la redirection)
     * @param contactId          l'identifiant du contact à supprimer
     * @param redirectAttributes les attributs flash transmis à la redirection
     * @return une redirection vers la fiche de l'entreprise
     */
    @PostMapping("/entreprises/{entrepriseId}/contacts/{contactId}/supprimer")
    public String supprimerContact(
            @PathVariable Integer entrepriseId,
            @PathVariable Long contactId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            contactService.deleteContact(contactId);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/entreprises/" + entrepriseId;
    }

}
