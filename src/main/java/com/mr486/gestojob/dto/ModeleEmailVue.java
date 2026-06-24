package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'affichage d'un modèle d'email dans la page Paramètres (clé, libellé,
 * indicateur HTML, contenu courant et variables disponibles).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeleEmailVue {
    private String cle;
    private String libelleUi;
    private boolean html;
    private String contenu;
    private String variables;
}
