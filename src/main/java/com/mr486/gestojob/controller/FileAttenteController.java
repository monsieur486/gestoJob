package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Contrôleur MVC gérant la file d'attente des annonces dont l'e-mail reste à
 * envoyer : affichage de la file, envoi groupé et envoi individuel.
 */
@Controller
@RequiredArgsConstructor
public class FileAttenteController {

    private final AnnonceService annonceService;

    /**
     * Affiche la liste des annonces en attente d'envoi d'e-mail.
     *
     * @param model le modèle Thymeleaf alimenté pour la vue
     * @return le nom de la vue Thymeleaf {@code file}
     *
     * <p><b>Exemple :</b> GET /file place la liste des annonces renvoyées par annoncesEnAttenteEnvoiEmail() dans le modèle et retourne la vue {@code file}.</p>
     */
    @GetMapping("/file")
    public String fileAttenteView(Model model) {
        model.addAttribute("page_active", "file");
        model.addAttribute("annoncesEnAttenteEnvoiEmail", annonceService.annoncesEnAttenteEnvoiEmail());
        return "file";
    }

    /**
     * Déclenche l'envoi des e-mails pour toutes les annonces en attente.
     *
     * @return une redirection vers la page {@code /file}
     *
     * <p><b>Exemple :</b> POST /file/postMail envoie les e-mails de toutes les annonces en attente via sendEmailForPendingAnnonces() puis redirige vers /file.</p>
     */
    @PostMapping("/file/postMail")
    public String postMail() {
        annonceService.sendEmailForPendingAnnonces();
        return "redirect:/file";
    }

    /**
     * Déclenche l'envoi de l'e-mail pour une annonce précise.
     *
     * @param annonceId l'identifiant de l'annonce dont l'e-mail doit être envoyé
     * @return une redirection vers la page {@code /file}
     *
     * <p><b>Exemple :</b> POST /file/postMail/7 envoie l'e-mail de l'annonce 7 via sendDirectEmail(7) puis redirige vers /file.</p>
     */
    @PostMapping("/file/postMail/{annonceId}")
    public String postDirectMail(@PathVariable Long annonceId) {
        annonceService.sendDirectEmail(annonceId);
        return "redirect:/file";
    }
}
