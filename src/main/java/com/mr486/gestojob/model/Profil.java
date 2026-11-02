package com.mr486.gestojob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entité JPA du profil d'un titulaire (table {@code profil}) : nom du candidat,
 * libellé d'affichage et intitulé de poste par défaut de chacune des trois
 * lettres (général, microservices, IA agentique).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "profil")
public class Profil {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Titulaire titulaire;

    @Column(name = "candidat_nom", nullable = false)
    private String candidatNom;

    @Column(name = "libelle_general", nullable = false, length = 64)
    private String libelleGeneral;

    @Column(name = "poste_general", nullable = false)
    private String posteGeneral;

    @Column(name = "libelle_microservices", nullable = false, length = 64)
    private String libelleMicroservices;

    @Column(name = "poste_microservices", nullable = false)
    private String posteMicroservices;

    @Column(name = "libelle_ia", nullable = false, length = 64)
    private String libelleIa;

    @Column(name = "poste_ia", nullable = false)
    private String posteIa;
}
