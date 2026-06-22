package com.mr486.gestojob.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlToPlainTextTest {

    @Test
    void renvoieChaineVide_pourEntreeNulleOuVide() {
        assertThat(HtmlToPlainText.toPlainTextKeepLines(null)).isEmpty();
        assertThat(HtmlToPlainText.toPlainTextKeepLines("   ")).isEmpty();
    }

    @Test
    void supprimeLesBalisesHtml() {
        String html = "<p>Bonjour <strong>Monde</strong></p>";
        assertThat(HtmlToPlainText.toPlainTextKeepLines(html)).isEqualTo("Bonjour Monde");
    }

    @Test
    void conserveLesSautsDeLigneEntreParagraphes() {
        String html = "<p>Ligne 1</p><p>Ligne 2</p>";
        String text = HtmlToPlainText.toPlainTextKeepLines(html);
        assertThat(text).contains("Ligne 1").contains("Ligne 2");
        assertThat(text).contains("\n");
    }

    @Test
    void limiteLesSautsDeLigneMultiples() {
        String html = "<p>A</p><p></p><p></p><p>B</p>";
        assertThat(HtmlToPlainText.toPlainTextKeepLines(html)).doesNotContain("\n\n\n");
    }
}
