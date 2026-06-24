package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.CleModele;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Service de génération des contenus de candidature (lettres de motivation).
 * Produit, selon le type de poste, la version HTML et la version texte du message
 * en substituant la formule de politesse et l'intitulé du poste dans le modèle
 * éditable correspondant (lu via {@link ModeleEmailService}). Le texte est dérivé
 * du HTML (source unique).
 */
@Service
@RequiredArgsConstructor
public class ContenuService {

    private final HtmlConverterService htmlConverterService;
    private final ModeleEmailService modeleEmailService;

    /**
     * Génère la version HTML du contenu de la candidature.
     *
     * <p><b>Exemple :</b> un poste contenant « &lt;script&gt; » est échappé dans le
     * HTML produit.</p>
     *
     * @param poste              intitulé du poste (valeur par défaut si vide)
     * @param typeContenu        type de contenu (1 = microservices, 2 = IA, sinon général)
     * @param messageDePolitesse formule de politesse (salutation générique si null)
     * @return le contenu HTML
     */
    public String getHtmlContenu(String poste, Integer typeContenu, String messageDePolitesse) {
        return getContent(poste, typeContenu, messageDePolitesse, true);
    }

    /**
     * Génère la version texte du contenu de la candidature (dérivée du HTML).
     *
     * <p><b>Exemple :</b> avec typeContenu=1 et un poste vide, le texte utilise le
     * poste par défaut « de développeur Java orienté microservices » ; le poste
     * fourni n'est pas échappé.</p>
     *
     * @param poste              intitulé du poste (valeur par défaut si vide)
     * @param typeContenu        type de contenu (1 = microservices, 2 = IA, sinon général)
     * @param messageDePolitesse formule de politesse (salutation générique si null)
     * @return le contenu texte
     */
    public String getTextContenu(String poste, Integer typeContenu, String messageDePolitesse) {
        return getContent(poste, typeContenu, messageDePolitesse, false);
    }

    // Lit le modèle HTML correspondant au type, puis substitue politesse et poste.
    // HTML : saisies échappées (anti-injection). Texte : modèle converti en texte
    // brut (HtmlToPlainText) puis substitution non échappée.
    private String getContent(String poste, Integer typeContenu, String messageDePolitesse, boolean isHtml) {
        CleModele cle = CleModele.pourTypeContenu(typeContenu);
        String modeleHtml = modeleEmailService.getContenu(cle.name());
        String politesse = (messageDePolitesse == null)
                ? ApplicationConfiguration.SALUTATION_GENERIQUE
                : messageDePolitesse;
        String posteDefaut = posteParDefaut(cle);

        if (isHtml) {
            String safePoste = (poste == null || poste.isEmpty()) ? posteDefaut : poste;
            return modeleHtml
                    .replace("{{POLITESSE}}", HtmlUtils.htmlEscape(politesse))
                    .replace("{{POSTE}}", HtmlUtils.htmlEscape(safePoste));
        }

        String texte = htmlConverterService.htmlToPlainText(modeleHtml);
        String posteTexte = (poste == null || poste.isEmpty()) ? posteDefaut : poste;
        return texte
                .replace("{{POLITESSE}}", politesse)
                .replace("{{POSTE}}", posteTexte);
    }

    // Intitulé de poste par défaut selon le modèle.
    private String posteParDefaut(CleModele cle) {
        return switch (cle) {
            case CONTENU_MICROSERVICES -> ApplicationConfiguration.POSTE_DEFAUT_MICROSERVICES;
            case CONTENU_IA -> ApplicationConfiguration.POSTE_DEFAUT_IA;
            default -> ApplicationConfiguration.POSTE_DEFAUT_GENERAL;
        };
    }
}
