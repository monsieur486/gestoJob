package com.mr486.gestojob.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'affichage d'un modèle d'email dans la page Paramètres (clé, libellé,
 * indicateur HTML, contenu courant, variables disponibles et variables obligatoires).
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
    // Variables à insérer par bouton et exigées à l'enregistrement (vide pour un objet).
    private List<String> variablesObligatoires;
}
