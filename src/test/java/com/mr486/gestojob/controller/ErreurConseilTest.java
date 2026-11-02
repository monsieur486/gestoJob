package com.mr486.gestojob.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

class ErreurConseilTest {

    private final ErreurConseil conseil = new ErreurConseil();

    @Test
    void introuvable_rendLaPage404() {
        ModelAndView vue = conseil.introuvable(new NoSuchElementException("Entreprise introuvable avec id: 9"));

        assertThat(vue.getViewName()).isEqualTo("erreur-404");
        assertThat(vue.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void introuvable_neRevelePasLeMessageDeLException() {
        // Un id inexistant et un id d'un autre titulaire lèvent des messages potentiellement
        // différents : la vue ne doit en transporter aucun, pour rester indiscernable.
        ModelAndView vue = conseil.introuvable(new NoSuchElementException("message interne"));

        assertThat(vue.getModel()).isEmpty();
    }
}
