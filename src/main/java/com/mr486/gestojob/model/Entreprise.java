package com.mr486.gestojob.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entité JPA représentant une entreprise.
 * Elle est persistée dans la table {@code entreprises} et contient les
 * coordonnées de l'entreprise (nom, adresse, code postal, ville) ainsi que
 * son indicateur d'activité.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "entreprises")
public class Entreprise {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Boolean estActive;
    private String nom;
    private String adresse1;
    private String adresse2;
    private String codePostal;
    private String ville;
}
