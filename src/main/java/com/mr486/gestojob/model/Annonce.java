package com.mr486.gestojob.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

/**
 * Entité JPA représentant une annonce (candidature ou demande spontanée)
 * adressée à une entreprise. Elle est persistée dans la table {@code annonces}
 * et conserve les informations relatives au poste visé, au type d'annonce,
 * au contact associé ainsi qu'au statut de suivi de la candidature.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "annonces")
public class Annonce {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private Integer entrepriseId;

    private Long contactId;
    private Integer typeAnnonce; // type d'annonce : 1 = candidature à une offre, sinon = demande spontanée
    private Integer typeContenu; // identifiant du type de contenu associé à l'annonce
    private String poste;
    private String reference;
    // statut de suivi : 1=Boîte d'envoi, 2=En cours, 3=Dépassé, 4=Négatif, 5=Positif, 6=Archivé
    private Integer statusAnnonce;
    private OffsetDateTime dateEnvoi;

    /**
     * Retourne le libellé lisible (avec icône) correspondant au statut numérique
     * de l'annonce.
     *
     * <p><b>Exemple :</b> pour statusAnnonce = 5, retourne « 🟢 Positif ».</p>
     *
     * @return le libellé du statut, ou {@code "Inconnu"} si le code de statut
     *         n'est pas reconnu
     */
    public String getStatusAnnonceString() {
        return StatutAnnonce.libelle(statusAnnonce);
    }

}
