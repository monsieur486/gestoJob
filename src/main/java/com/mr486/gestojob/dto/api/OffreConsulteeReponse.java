package com.mr486.gestojob.dto.api;

import java.time.LocalDate;

/**
 * Réponse de l'API après l'enregistrement d'une offre consultée.
 *
 * <p><b>Exemple :</b> {@code {"id":12,"dateConsultation":"2026-11-18","motif":"DISTANCE"}}.</p>
 *
 * @param id               l'identifiant de l'entrée créée
 * @param dateConsultation la date retenue
 * @param motif            le nom du motif d'écart retenu
 */
public record OffreConsulteeReponse(Long id, LocalDate dateConsultation, String motif) {
}
