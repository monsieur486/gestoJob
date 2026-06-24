package com.mr486.gestojob.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Nettoyage du HTML des modèles d'email : ne laisse passer qu'un jeu restreint de
 * balises de mise en forme (gras, italique, soulignement, paragraphes, listes,
 * liens), pour éviter toute injection de HTML/JS dans les emails et normaliser la
 * sortie de l'éditeur.
 */
public final class HtmlSanitizer {

    // Safelist restreint : balises de mise en forme uniquement, sans attributs dangereux.
    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "ul", "ol", "li", "a")
            .addAttributes("a", "href", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto");

    private HtmlSanitizer() {
    }

    /**
     * Nettoie un fragment HTML en ne conservant que les balises autorisées.
     *
     * <p><b>Exemple :</b> {@code nettoie("<p onclick=x>a</p><script>b</script>")}
     * retourne « &lt;p&gt;a&lt;/p&gt; » (script et attribut retirés) ; les variables
     * {{...}} sont préservées ; une entrée nulle retourne une chaîne vide.</p>
     *
     * @param html le HTML à nettoyer (peut être nul)
     * @return le HTML nettoyé, ou une chaîne vide si l'entrée est nulle
     */
    public static String nettoie(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, "", SAFELIST,
                new Document.OutputSettings().prettyPrint(false));
    }
}
