package com.mr486.gestojob.dto;

import jakarta.validation.constraints.Size;
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
    // Renseigné par le contrôleur depuis le chemin (/entreprises/{id}/...), pas
    // par le formulaire : donc PAS de @NotNull ici (la validation @Valid s'exécute
    // avant que l'id ne soit posé). Le service garde un contrôle anti-null.
    private Integer entrepriseId;
    private Integer contenuId = 2; // 0 = général, 1 = microservices, 2 = IA agentique (par défaut)
    private Long contactId;
    private Integer typeAnnonce = 0; // type d'annonce : 1 = candidature à une offre, sinon = demande spontanée
    @Size(max = 255, message = "Le poste ne doit pas dépasser 255 caractères.")
    private String poste;
    @Size(max = 255, message = "La référence ne doit pas dépasser 255 caractères.")
    private String reference;
    private OffsetDateTime dateEnvoi;
}
