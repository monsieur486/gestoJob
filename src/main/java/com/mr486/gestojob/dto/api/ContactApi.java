package com.mr486.gestojob.dto.api;

/**
 * Contact d'une entreprise tel qu'exposé par l'API de recherche.
 *
 * <p><b>Exemple :</b> {@code new ContactApi(7L, "Durand", "d@acme.fr", 2)} décrit Madame Durand.</p>
 *
 * @param id                  identifiant du contact
 * @param nom                 nom du contact (peut être nul)
 * @param email               adresse email du contact
 * @param formuleDePolitesse  1 = Monsieur, 2 = Madame, autre = Madame, Monsieur
 */
public record ContactApi(Long id, String nom, String email, Integer formuleDePolitesse) {
}
