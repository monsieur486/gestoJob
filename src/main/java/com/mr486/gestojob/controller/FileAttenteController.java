package com.mr486.gestojob.controller;

import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.service.AnnonceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
     * <p><b>Exemple :</b> GET /file?page=2 place la page 2 (index 1) des annonces en attente dans le modèle et retourne la vue {@code file}.</p>
     *
     * @param model le modèle Thymeleaf alimenté pour la vue
     * @param page  le numéro de page demandé (commence à 1)
     * @return le nom de la vue Thymeleaf {@code file}
     */
    @GetMapping("/file")
    public String fileAttenteView(
            Model model,
            @RequestParam(name = "page", defaultValue = "1") int page
    ) {
        model.addAttribute("page_active", "file");
        int pageIndex = Math.max(0, page - 1);
        Page<AnnonceListe> pageResult = annonceService.annoncesEnAttenteEnvoiEmailPage(pageIndex);
        model.addAttribute("annoncesEnAttenteEnvoiEmail", pageResult.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        return "file";
    }

    /**
     * Déclenche l'envoi des e-mails pour toutes les annonces en attente.
     *
     * <p><b>Exemple :</b> POST /file/postMail envoie les e-mails de toutes les annonces en attente via sendEmailForPendingAnnonces() puis redirige vers /file.</p>
     *
     * @return une redirection vers la page {@code /file}
     */
    @PostMapping("/file/postMail")
    public String postMail() {
        annonceService.sendEmailForPendingAnnonces();
        return "redirect:/file";
    }

    /**
     * Déclenche l'envoi de l'e-mail pour une annonce précise.
     *
     * <p><b>Exemple :</b> POST /file/postMail/7 envoie l'e-mail de l'annonce 7 via sendDirectEmail(7) puis redirige vers /file.</p>
     *
     * @param annonceId l'identifiant de l'annonce dont l'e-mail doit être envoyé
     * @return une redirection vers la page {@code /file}
     */
    @PostMapping("/file/postMail/{annonceId}")
    public String postDirectMail(@PathVariable Long annonceId) {
        annonceService.sendDirectEmail(annonceId);
        return "redirect:/file";
    }
}
