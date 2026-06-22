package com.mr486.gestojob.service;

import com.mr486.gestojob.tools.HtmlToPlainText;
import org.springframework.stereotype.Service;

/**
 * Service de conversion de contenu HTML en texte brut.
 */
@Service
public class HtmlConverterService {
    /**
     * Convertit du HTML en texte brut en conservant les sauts de ligne.
     *
     * @param html le contenu HTML à convertir
     * @return le texte brut correspondant
     *
     * <p><b>Exemple :</b> htmlToPlainText("&lt;p&gt;Bonjour&lt;/p&gt;&lt;p&gt;Au revoir&lt;/p&gt;") retourne le texte « Bonjour » et « Au revoir » séparés par un saut de ligne.</p>
     */
    public String htmlToPlainText(String html) {
        return HtmlToPlainText.toPlainTextKeepLines(html);
    }
}
