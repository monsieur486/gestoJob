package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.AnnonceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContenuController {

    private final AnnonceService annonceService;

    @GetMapping(value = "/contenu/{id}", produces = "text/plain; charset=UTF-8")
    public String getContenuById(@PathVariable Long id) {
        return annonceService.getAnnonceTxtContenuById(id);
    }
}
