package com.mr486.gestojob.model;

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
    private Integer formuleDePolistesse; // formule de politesse : 1 = Monsieur, 2 = Madame, autre = Madame, Monsieur
    private String contact;

    /**
     * Construit le message de politesse (en-tête de courrier) adapté au contact,
     * en fonction de la formule de politesse renseignée.
     *
     * <p><b>Exemple :</b> formuleDePolistesse = 2 et contact = « Durand » donne « Madame Durand, » ; formuleDePolistesse = 1 donne « Monsieur Durand, » ; toute autre valeur donne « Madame, Monsieur, ».</p>
     *
     * @return la formule de politesse formatée ; {@code "Madame, Monsieur,"}
     *         lorsque aucune formule spécifique n'est définie
     */
    public String getMessageDePolitesse() {
        String message = "";
        if (formuleDePolistesse == 2) {
            message = "Madame " + contact + ",";
        } else if (formuleDePolistesse == 1) {
            message = "Monsieur " + contact + ",";
        } else {
            message = "Madame, Monsieur,";
        }

        return message;
    }
}
