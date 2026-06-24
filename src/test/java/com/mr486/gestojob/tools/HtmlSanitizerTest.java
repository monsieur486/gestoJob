package com.mr486.gestojob.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void conserveLesBalisesAutorisees() {
        String html = "<p>Bonjour <strong>tout</strong> le monde</p><ul><li>un</li></ul>";
        String resultat = HtmlSanitizer.nettoie(html);
        assertThat(resultat).contains("<p>").contains("<strong>").contains("<ul>").contains("<li>");
    }

    @Test
    void retireScriptEtAttributsDangereux() {
        String html = "<p onclick=\"vol()\">x</p><script>alert(1)</script>";
        String resultat = HtmlSanitizer.nettoie(html);
        assertThat(resultat).doesNotContain("script").doesNotContain("onclick");
    }

    @Test
    void preserveLesVariables() {
        String html = "<p>{{POLITESSE}}</p><p>poste {{POSTE}}</p>";
        String resultat = HtmlSanitizer.nettoie(html);
        assertThat(resultat).contains("{{POLITESSE}}").contains("{{POSTE}}");
    }

    @Test
    void entreeNulle_retourneChaineVide() {
        assertThat(HtmlSanitizer.nettoie(null)).isEmpty();
    }
}
