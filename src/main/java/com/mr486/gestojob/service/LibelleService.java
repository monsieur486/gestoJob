package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.CleModele;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Construit l'objet (libellé) d'un email de candidature à partir du modèle de
 * libellé adapté au type d'annonce, en substituant la référence, le nom du
 * candidat et le poste. Isolé de l'entité {@link Annonce} (qui ne doit pas
 * dépendre de la base).
 */
@Service
@RequiredArgsConstructor
public class LibelleService {

    private final ModeleEmailService modeleEmailService;

    /**
     * Construit le libellé d'une annonce.
     *
     * <p><b>Exemple :</b> pour une annonce de type référence (reference=« ABC123 »,
     * poste=« Développeur »), retourne « Réf [ABC123] Laurent Touret - candidature
     * au poste Développeur » ; pour une spontanée, le libellé spontané.</p>
     *
     * @param annonce l'annonce concernée
     * @return le libellé prêt à l'emploi (variables substituées)
     */
    public String construitLibelle(Annonce annonce) {
        CleModele cle = CleModele.pourTypeAnnonce(annonce.getTypeAnnonce());
        return modeleEmailService.getContenu(cle.name())
                .replace("{{REFERENCE}}", valeur(annonce.getReference()))
                .replace("{{NOM}}", ApplicationConfiguration.CANDIDAT_NOM)
                .replace("{{POSTE}}", valeur(annonce.getPoste()));
    }

    // Remplace une valeur nulle par une chaîne vide pour éviter « null » dans le libellé.
    private String valeur(String valeur) {
        return valeur == null ? "" : valeur;
    }
}
