package com.mr486.gestojob.model;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entité JPA représentant un contact rattaché à une entreprise.
 * Elle est persistée dans la table {@code contacts} et stocke l'email,
 * le nom du contact ainsi que la formule de politesse à utiliser lors
 * des échanges.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "contacts")
public class Contact {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private Integer entrepriseId;
    @NotNull
    private String email;
    private Integer formuleDePolitesse; // formule de politesse : 1 = Monsieur, 2 = Madame, autre = Madame, Monsieur
    private String nom;

    /**
     * Construit le message de politesse (en-tête de courrier) adapté au contact,
     * en fonction de la formule de politesse renseignée.
     *
     * <p><b>Exemple :</b> formuleDePolitesse = 2 et nom = « Durand » donne « Madame Durand, » ; formuleDePolitesse = 1 sans nom (null ou vide) donne « Monsieur, » ; formuleDePolitesse null ou toute autre valeur donne « Madame, Monsieur, ».</p>
     *
     * @return la formule de politesse formatée ; {@code "Madame, Monsieur,"}
     *         lorsque aucune formule spécifique n'est définie
     */
    public String getMessageDePolitesse() {
        // Suffixe « Nom » uniquement si un nom exploitable est renseigné, pour
        // éviter une salutation du type « Monsieur null, » dans le corps de l'email.
        String suffixeNom = (nom != null && !nom.isBlank()) ? " " + nom.trim() : "";
        if (Integer.valueOf(2).equals(formuleDePolitesse)) {
            return ApplicationConfiguration.CIVILITE_MADAME + suffixeNom + ",";
        }
        if (Integer.valueOf(1).equals(formuleDePolitesse)) {
            return ApplicationConfiguration.CIVILITE_MONSIEUR + suffixeNom + ",";
        }
        return ApplicationConfiguration.SALUTATION_GENERIQUE;
    }
}
