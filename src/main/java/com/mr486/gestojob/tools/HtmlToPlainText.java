package com.mr486.gestojob.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HtmlToPlainText {
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
