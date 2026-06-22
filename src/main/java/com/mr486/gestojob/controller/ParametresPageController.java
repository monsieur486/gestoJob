package com.mr486.gestojob.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ParametresPageController {

    @GetMapping("/parametres")
    public String parametresView(Model model) {
        model.addAttribute("page_active", "parametres");
        return "parametres";
    }
}
