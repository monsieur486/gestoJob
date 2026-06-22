package com.mr486.gestojob.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Utilitaire de conversion de contenu HTML en texte brut.
 * <p>
 * Permet de transformer un fragment HTML en texte lisible tout en préservant
 * une structure de lignes raisonnable (sauts de ligne, puces de liste,
 * tabulations) et en nettoyant les espaces et lignes superflus.
 */
public class HtmlToPlainText {
    /**
     * Convertit un contenu HTML en texte brut tout en conservant une mise en
     * forme par lignes.
     * <p>
     * Les balises de structure ({@code <br>}, {@code <p>}, {@code <div>}, titres,
     * éléments de liste, lignes et cellules de tableau) sont transformées en
     * sauts de ligne, puces ou tabulations, puis le résultat est nettoyé pour
     * réduire les espaces et les sauts de ligne excessifs.
     *
     * <p><b>Exemple :</b> « &lt;p&gt;Bonjour &lt;b&gt;Monde&lt;/b&gt;&lt;/p&gt; » devient « Bonjour Monde » ; une entrée {@code null} ou vide renvoie une chaîne vide.</p>
     *
     * @param html le contenu HTML à convertir (peut être {@code null} ou vide)
     * @return le texte brut correspondant, ou une chaîne vide si l'entrée est nulle ou vide
     */
    public static String toPlainTextKeepLines(String html) {
        if (html == null || html.isBlank()) return "";

        Document doc = Jsoup.parse(html);

        // Évite d'avoir tout collé à cause des <br> etc.
        doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

        // Convertit certains tags en retours ligne
        doc.select("br").append("\\n");
        doc.select("p, div, section, article, header, footer, h1,h2,h3,h4,h5,h6").append("\\n\\n");
        doc.select("li").prepend("• ").append("\\n");
        doc.select("tr").append("\\n");
        doc.select("th, td").append("\\t"); // ou " | " selon ton rendu

        // Récupère le texte avec les \n injectés
        String text = doc.text();

        // Remet les vrais retours à la ligne
        text = text.replace("\\n", "\n");

        // Nettoyage : espaces et lignes multiples
        text = text
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")     // espaces multiples -> 1
                .replaceAll("\\n{3,}", "\n\n")            // trop de sauts -> 2 max
                .trim();

        return text;
    }
}
