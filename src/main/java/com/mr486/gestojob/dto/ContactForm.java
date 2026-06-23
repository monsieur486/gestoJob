package com.mr486.gestojob.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de formulaire utilisé pour la création ou la modification d'un contact.
 * Il porte les contraintes de validation sur l'email et regroupe les données
 * saisies (email, formule de politesse, nom du contact) avant conversion en
 * entité {@link com.mr486.gestojob.model.Contact}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactForm {

    // Email facultatif au niveau du formulaire (un contact « site » n'en a pas) ;
    // s'il est fourni, son format doit être valide. L'obligation lorsqu'une formule
    // de politesse est choisie est portée par ContactService.saveContact.
    @Email(message = "Le format de l'email est invalide.")
    private String email;
    private Integer formuleDePolitesse = 0; // formule de politesse : 1 = Monsieur, 2 = Madame, autre = Madame, Monsieur
    private String nom;
}
