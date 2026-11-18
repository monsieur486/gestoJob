package com.mr486.gestojob.dto.api;

/**
 * Contact visé par une demande de candidature : soit existant (identifiant
 * renseigné), soit à créer (email, nom et formule de politesse). Lorsque l'identifiant
 * est renseigné, l'email, le nom et la formule de politesse sont ignorés.
 *
 * <p><b>Exemple :</b> {@code new ContactDemande(null, "d@acme.fr", "Durand", 2)} crée Madame Durand.</p>
 *
 * @param id                 identifiant d'un contact existant (facultatif)
 * @param email              email du contact à créer
 * @param nom                nom du contact à créer
 * @param formuleDePolitesse 1 = Monsieur, 2 = Madame, autre = Madame, Monsieur
 */
public record ContactDemande(Long id, String email, String nom, Integer formuleDePolitesse) {
}
