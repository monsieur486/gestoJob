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

@Controller
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/entreprises/{id}/contacts/ajout")
    public String ajoutEntrepriseView(Model model) {
        model.addAttribute("page_active", "entreprises");
        model.addAttribute("form", new ContactForm());
        return "add-contact";
    }

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
