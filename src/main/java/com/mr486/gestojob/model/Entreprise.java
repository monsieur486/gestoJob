package com.mr486.gestojob.model;

import jakarta.persistence.*;
import lombok.*;

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
