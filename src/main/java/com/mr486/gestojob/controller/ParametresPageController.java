package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.ProfilForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.service.ModeleEmailService;
import com.mr486.gestojob.service.ProfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur MVC de la page des paramètres : affichage et édition des modèles
 * d'email (corps de lettre et libellés).
 */
@Controller
@RequiredArgsConstructor
public class ParametresPageController {

    private final ModeleEmailService modeleEmailService;
    private final ProfilService profilService;

    /**
     * Affiche la page des paramètres avec les modèles éditables.
     *
     * <p><b>Exemple :</b> GET /parametres place la liste des modèles dans le modèle
     * et retourne la vue {@code parametres}.</p>
     *
     * @param model le modèle Thymeleaf
     * @return le nom de la vue {@code parametres}
     */
    @GetMapping("/parametres")
    public String parametresView(Model model) {
        model.addAttribute("page_active", "parametres");
        model.addAttribute("modeles", modeleEmailService.listerModeles());
        model.addAttribute("profilForm", ProfilForm.depuis(profilService.getProfilCourant()));
        return "parametres";
    }

    /**
     * Enregistre le profil (nom du candidat, libellés et postes par défaut des
     * lettres) du titulaire consulté. En cas d'erreur de validation, réaffiche la
     * page sans enregistrer.
     *
     * <p><b>Exemple :</b> POST /parametres/profil avec un nom vide réaffiche la vue
     * {@code parametres} ; avec un formulaire valide, enregistre puis redirige.</p>
     *
     * @param form               le formulaire de profil validé
     * @param bindingResult      le résultat de la validation
     * @param model              le modèle Thymeleaf (réaffichage en cas d'erreur)
     * @param redirectAttributes attributs flash
     * @return la vue {@code parametres} en cas d'erreur, sinon une redirection
     *         vers {@code /parametres}
     */
    @PostMapping("/parametres/profil")
    public String enregistrerProfil(@Valid @ModelAttribute("profilForm") ProfilForm form,
                                    BindingResult bindingResult,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("page_active", "parametres");
            model.addAttribute("modeles", modeleEmailService.listerModeles());
            return "parametres";
        }
        try {
            profilService.mettreAJour(form);
            redirectAttributes.addFlashAttribute("successMessage", "Profil enregistré.");
        } catch (MetierException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/parametres";
    }

    /**
     * Enregistre la nouvelle valeur d'un modèle puis redirige vers Paramètres.
     *
     * <p><b>Exemple :</b> POST /parametres/modeles/CONTENU_GENERAL met à jour le
     * modèle puis redirige ; en cas d'erreur, un message flash est exposé.</p>
     *
     * @param cle                clé du modèle
     * @param contenu            nouveau contenu soumis
     * @param redirectAttributes attributs flash
     * @return une redirection vers {@code /parametres}
     */
    @PostMapping("/parametres/modeles/{cle}")
    public String enregistrerModele(@PathVariable String cle,
                                    @RequestParam("contenu") String contenu,
                                    RedirectAttributes redirectAttributes) {
        try {
            modeleEmailService.mettreAJour(cle, contenu);
            redirectAttributes.addFlashAttribute("successMessage", "Modèle enregistré.");
        } catch (MetierException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/parametres";
    }

    /**
     * Réinitialise un modèle à sa valeur par défaut puis redirige vers Paramètres.
     *
     * <p><b>Exemple :</b> POST /parametres/modeles/CONTENU_IA/reinitialiser restaure
     * le modèle IA d'origine puis redirige.</p>
     *
     * @param cle                clé du modèle
     * @param redirectAttributes attributs flash
     * @return une redirection vers {@code /parametres}
     */
    @PostMapping("/parametres/modeles/{cle}/reinitialiser")
    public String reinitialiserModele(@PathVariable String cle,
                                      RedirectAttributes redirectAttributes) {
        try {
            modeleEmailService.reinitialiser(cle);
            redirectAttributes.addFlashAttribute("successMessage", "Modèle réinitialisé.");
        } catch (MetierException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/parametres";
    }
}
