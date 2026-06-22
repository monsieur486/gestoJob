package com.mr486.gestojob.model;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

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
    private Integer typeAnnonce;
    private Integer typeContenu;
    private String poste;
    private String reference;
    private Integer statusAnnonce;
    private OffsetDateTime dateEnvoi;

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

    public String getLibelle() {
        if (typeAnnonce == 1) {
            return "Réf [" + reference + "] Laurent Touret - candidature au poste " + poste;
        } else {
            return ApplicationConfiguration.DEMANDE_SPONTANEE_TXT;
        }
    }
}
