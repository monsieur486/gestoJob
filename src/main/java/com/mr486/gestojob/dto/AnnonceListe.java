package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de présentation représentant une ligne d'annonce dans les listes
 * affichées à l'utilisateur. Il contient des champs déjà formatés pour
 * l'affichage (date, contact, type, libellé, statut) issus de l'entité
 * {@link com.mr486.gestojob.model.Annonce}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnonceListe {
    private Long id;
    private Integer entrepriseId;
    private String info;
    private String dateEnvoi;
    private String contact;
    private String type;
    private String libelle;
    private String status;
}
