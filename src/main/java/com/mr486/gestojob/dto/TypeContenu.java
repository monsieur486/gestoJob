package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO représentant un type de contenu d'annonce, associant un identifiant
 * à son étiquette lisible. Il est notamment utilisé pour alimenter les listes
 * de sélection proposées à l'utilisateur.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeContenu {

    private Integer id;
    private String etiquette;
}
