package com.mr486.gestojob.model;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entité JPA représentant une annonce (candidature ou demande spontanée)
 * adressée à une entreprise. Elle est persistée dans la table {@code annonces}
 * et conserve les informations relatives au poste visé, au type d'annonce,
 * au contact associé ainsi qu'au statut de suivi de la candidature.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "annonces")
public class Annonce {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private Integer entrepriseId;

    private Long contactId;
    private Integer typeAnnonce; // type d'annonce : 1 = candidature à une offre, sinon = demande spontanée
    private Integer typeContenu; // identifiant du type de contenu associé à l'annonce
    private String poste;
    private String reference;
    private Integer statusAnnonce; // statut de suivi : 1=Boîte d'envoi, 2=En cours, 3=Dépassé, 4=Négatif, 5=Positif, 6=Archivé
    private OffsetDateTime dateEnvoi;

    /**
     * Retourne le libellé lisible (avec icône) correspondant au statut numérique
     * de l'annonce.
     *
     * <p><b>Exemple :</b> pour statusAnnonce = 5, retourne « 🟢 Positif ».</p>
     *
     * @return le libellé du statut, ou {@code "Inconnu"} si le code de statut
     *         n'est pas reconnu
     */
    public String getStatusAnnonceString() {
        return switch (statusAnnonce) {
            case 1 -> "\uD83D\uDD82 Boîte d'envoi";
            case 2 -> "\uD83D\uDFE0 En cours";
            case 3 -> "⏳ Dépassé";
            case 4 -> "\uD83D\uDD34 Négatif";
            case 5 -> "\uD83D\uDFE2 Positif";
            case 6 -> "\uD83D\uDCE6 Archivé";
            default -> "Inconnu";
        };
    }

    /**
     * Construit le libellé de l'annonce destiné à l'affichage ou à l'envoi.
     * Pour une candidature à une offre ({@code typeAnnonce == 1}), le libellé
     * mentionne la référence et le poste ; sinon, il s'agit du texte standard
     * de demande spontanée.
     *
     * <p><b>Exemple :</b> pour typeAnnonce = 1, reference = « ABC123 » et poste = « Développeur », retourne « Réf [ABC123] Laurent Touret - candidature au poste Développeur » ; sinon, retourne le texte standard de demande spontanée ({@code ApplicationConfiguration.DEMANDE_SPONTANEE_TXT}).</p>
     *
     * @return le libellé textuel de l'annonce
     */
    public String getLibelle() {
        if (typeAnnonce == 1) {
            return "Réf [" + reference + "] Laurent Touret - candidature au poste " + poste;
        } else {
            return ApplicationConfiguration.DEMANDE_SPONTANEE_TXT;
        }
    }
}
