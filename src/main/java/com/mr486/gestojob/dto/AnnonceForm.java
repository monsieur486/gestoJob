package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO de formulaire utilisé pour la création ou la modification d'une annonce.
 * Il regroupe les données saisies par l'utilisateur (entreprise, contact,
 * type d'annonce, poste, référence, date d'envoi) avant leur conversion en
 * entité {@link com.mr486.gestojob.model.Annonce}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnonceForm {
    private Integer entrepriseId;
    private Integer contenuId = 0;
    private Long contactId;
    private Integer typeAnnonce = 0; // type d'annonce : 1 = candidature à une offre, sinon = demande spontanée
    private String poste;
    private String reference;
    private OffsetDateTime dateEnvoi;
}
