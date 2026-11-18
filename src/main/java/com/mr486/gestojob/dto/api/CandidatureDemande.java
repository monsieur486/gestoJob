package com.mr486.gestojob.dto.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête de création d'une candidature par l'API : entreprise,
 * contact éventuel, annonce et lettre de motivation personnalisée.
 *
 * <p><b>Exemple :</b> sans contact, la candidature est de type « site » (déjà envoyée).</p>
 *
 * @param entreprise entreprise existante ou à créer
 * @param contact    contact existant ou à créer ; nul pour une candidature « site »
 * @param annonce    données de l'annonce
 * @param lettreHtml lettre en HTML, contenant {@code {{POLITESSE}}}
 */
public record CandidatureDemande(
        @NotNull(message = "L'entreprise est obligatoire.") @Valid EntrepriseDemande entreprise,
        @Valid ContactDemande contact,
        @NotNull(message = "L'annonce est obligatoire.") @Valid AnnonceDemande annonce,
        @NotBlank(message = "La lettre est obligatoire.") String lettreHtml) {
}
