package com.mr486.gestojob.dto.api;

/**
 * Réponse à la création d'une candidature : identifiants créés ou utilisés et
 * indicateurs de ce qui a été créé ou réactivé.
 *
 * <p><b>Exemple :</b> {@code new CandidatureReponse(5L, 31, 77L, 1, true, false, true)}.</p>
 *
 * @param annonceId           identifiant de l'annonce créée
 * @param entrepriseId        identifiant de l'entreprise
 * @param contactId           identifiant du contact (nul si aucun)
 * @param statut              code du statut de l'annonce (1 = boîte d'envoi, 2 = en cours)
 * @param entrepriseCreee     {@code true} si l'entreprise a été créée
 * @param entrepriseReactivee {@code true} si l'entreprise était inactive et a été réactivée
 * @param contactCree         {@code true} si le contact a été créé
 */
public record CandidatureReponse(Long annonceId, Integer entrepriseId, Long contactId, Integer statut,
                                 boolean entrepriseCreee, boolean entrepriseReactivee, boolean contactCree) {
}
