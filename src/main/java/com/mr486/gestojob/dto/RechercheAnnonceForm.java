package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechercheAnnonceForm {

    private String recherche;
    private Boolean avecArchives;
}
