package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class FileAttenteController {

    private final AnnonceService annonceService;

    @GetMapping("/file")
    public String fileAttenteView(Model model) {
        model.addAttribute("page_active", "file");
        model.addAttribute("annoncesEnAttenteEnvoiEmail", annonceService.annoncesEnAttenteEnvoiEmail());
        return "file";
    }

    @PostMapping("/file/postMail")
    public String postMail() {
        annonceService.sendEmailForPendingAnnonces();
        return "redirect:/file";
    }

    @PostMapping("/file/postMail/{annonceId}")
    public String postDirectMail(@PathVariable Long annonceId) {
        annonceService.sendDirectEmail(annonceId);
        return "redirect:/file";
    }
}
