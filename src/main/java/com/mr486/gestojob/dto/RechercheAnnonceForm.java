package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de formulaire de recherche d'annonces. Il porte le critère de recherche
 * textuel ainsi que l'option permettant d'inclure ou non les annonces
 * archivées dans les résultats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechercheAnnonceForm {

    private String recherche;
    private Boolean avecArchives;
}
