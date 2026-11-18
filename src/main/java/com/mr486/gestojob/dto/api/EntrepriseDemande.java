package com.mr486.gestojob.dto.api;

/**
 * Entreprise visée par une demande de candidature : soit existante (identifiant
 * renseigné), soit à créer (nom, code postal et ville obligatoires).
 *
 * <p><b>Exemple :</b> {@code new EntrepriseDemande(12, null, null, null, null, null)} cible l'entreprise 12 ;
 * {@code new EntrepriseDemande(null, "Acme", null, null, "75001", "Paris")} en crée une nouvelle.</p>
 *
 * @param id         identifiant d'une entreprise existante (facultatif)
 * @param nom        nom de l'entreprise à créer
 * @param adresse1   première ligne d'adresse
 * @param adresse2   seconde ligne d'adresse
 * @param codePostal code postal
 * @param ville      ville
 */
public record EntrepriseDemande(Integer id, String nom, String adresse1, String adresse2,
                                String codePostal, String ville) {
}
