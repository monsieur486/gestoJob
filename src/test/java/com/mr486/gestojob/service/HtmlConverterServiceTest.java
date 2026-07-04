package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlConverterServiceTest {

    private final HtmlConverterService service = new HtmlConverterService();

    @Test
    void htmlToPlainText_supprimeLesBalises() {
        assertThat(service.htmlToPlainText("<p>Bonjour <b>Monde</b></p>")).isEqualTo("Bonjour Monde");
    }

    @Test
    void htmlToPlainText_renvoieVide_pourEntreeVide() {
        assertThat(service.htmlToPlainText("")).isEmpty();
    }
}
