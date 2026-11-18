package com.mr486.gestojob.dto.api;

import java.util.List;

/**
 * Entreprise telle qu'exposée par l'API de recherche, avec ses contacts.
 *
 * <p><b>Exemple :</b> {@code new EntrepriseApi(12, "Acme", "Paris", "75001", true, List.of())}.</p>
 *
 * @param id         identifiant de l'entreprise
 * @param nom        nom de l'entreprise
 * @param ville      ville
 * @param codePostal code postal
 * @param estActive  {@code true} si l'entreprise est active
 * @param contacts   contacts rattachés à l'entreprise
 */
public record EntrepriseApi(Integer id, String nom, String ville, String codePostal,
                            Boolean estActive, List<ContactApi> contacts) {
}
