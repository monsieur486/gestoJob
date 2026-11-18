package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.Titulaire;
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
    private final ProfilService profilService;

    /**
     * Génère la version HTML du contenu de la candidature.
     *
     * <p><b>Exemple :</b> un poste contenant « &lt;script&gt; » est échappé dans le
     * HTML produit.</p>
     *
     * @param annonce            annonce (titulaire, poste, type de contenu et lettre personnalisée éventuelle)
     * @param messageDePolitesse formule de politesse (salutation générique si null)
     * @return le contenu HTML
     */
    public String getHtmlContenu(Annonce annonce, String messageDePolitesse) {
        return getContent(annonce, messageDePolitesse, true);
    }

    /**
     * Génère la version texte du contenu de la candidature (dérivée du HTML).
     *
     * <p><b>Exemple :</b> avec typeContenu=1 et un poste vide, le texte utilise le
     * poste par défaut du profil (ex. « de développeur Java orienté microservices ») ; le poste
     * fourni n'est pas échappé.</p>
     *
     * @param annonce            annonce (titulaire, poste, type de contenu et lettre personnalisée éventuelle)
     * @param messageDePolitesse formule de politesse (salutation générique si null)
     * @return le contenu texte
     */
    public String getTextContenu(Annonce annonce, String messageDePolitesse) {
        return getContent(annonce, messageDePolitesse, false);
    }

    // Utilise la lettre personnalisée de l'annonce si elle est renseignée, sinon lit le
    // modèle HTML correspondant au type, puis substitue politesse et poste.
    // HTML : saisies échappées (anti-injection). Texte : modèle converti en texte
    // brut (HtmlToPlainText) puis substitution non échappée.
    private String getContent(Annonce annonce, String messageDePolitesse, boolean isHtml) {
        Titulaire titulaire = annonce.getTitulaire();
        String poste = annonce.getPoste();
        CleModele cle = CleModele.pourTypeContenu(annonce.getTypeContenu());
        String lettre = annonce.getLettrePersonnalisee();
        String modeleHtml = (lettre != null && !lettre.isBlank())
                ? lettre
                : modeleEmailService.getContenu(titulaire, cle.name());
        String politesse = (messageDePolitesse == null)
                ? ApplicationConfiguration.SALUTATION_GENERIQUE
                : messageDePolitesse;
        String posteDefaut = profilService.posteParDefaut(profilService.getProfil(titulaire), cle);

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
}
