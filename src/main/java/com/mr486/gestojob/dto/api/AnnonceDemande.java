package com.mr486.gestojob.dto.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Annonce à créer dans une demande de candidature.
 *
 * <p><b>Exemple :</b> {@code new AnnonceDemande(1, "Développeur Java", "REF-42")} : candidature à une référence.</p>
 *
 * @param typeAnnonce type d'annonce : 0 = spontanée, 1 = candidature à une référence
 * @param poste       intitulé du poste (255 caractères au plus)
 * @param reference   référence de l'offre (255 caractères au plus)
 */
public record AnnonceDemande(
        @NotNull(message = "Le type d'annonce est obligatoire.")
        @Min(value = 0, message = "Le type d'annonce doit valoir 0 (spontanée) ou 1 (référence).")
        @Max(value = 1, message = "Le type d'annonce doit valoir 0 (spontanée) ou 1 (référence).") Integer typeAnnonce,
        @Size(max = 255, message = "Le poste ne doit pas dépasser 255 caractères.") String poste,
        @Size(max = 255, message = "La référence ne doit pas dépasser 255 caractères.") String reference) {
}
