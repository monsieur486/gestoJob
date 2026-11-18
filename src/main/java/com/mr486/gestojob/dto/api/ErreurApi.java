package com.mr486.gestojob.dto.api;

/**
 * Corps d'erreur renvoyé par l'API.
 *
 * <p><b>Exemple :</b> {@code new ErreurApi("La ville est obligatoire.")}.</p>
 *
 * @param erreur message d'erreur destiné à l'appelant
 */
public record ErreurApi(String erreur) {
}
