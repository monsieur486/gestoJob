package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de présentation représentant une ligne d'entreprise dans les listes
 * affichées à l'utilisateur. Il expose une vue allégée de l'entité
 * {@link com.mr486.gestojob.model.Entreprise} (nom, identifiant et indicateur
 * d'activité).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseListe {
    private String nom;
    private Integer id;
    private Boolean estActive;
}
