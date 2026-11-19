package com.mr486.gestojob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Offre d'emploi lue puis écartée sans candidature, conservée pour montrer la
 * démarche de recherche (notamment au lecteur France Travail).
 *
 * <p><b>Exemple :</b> le 18/11/2026, « Développeur Java senior » chez Acme,
 * écartée pour le motif {@link MotifEcart#PROFIL} avec le commentaire
 * « exige 5 ans de Kotlin ».</p>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "offre_consultee")
public class OffreConsultee {

    /** Identifiant technique. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Titulaire propriétaire de l'entrée. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Titulaire titulaire;

    /** Date à laquelle l'offre a été lue. */
    @Column(nullable = false)
    private LocalDate dateConsultation;

    /** Intitulé du poste proposé. */
    @Column(nullable = false)
    private String intitule;

    /** Nom de l'entreprise, en texte libre (pas de lien avec la table des entreprises). */
    @Column(nullable = false)
    private String entreprise;

    /** Lien vers l'offre ({@code http(s)://}), facultatif. */
    @Column(length = 1000)
    private String lien;

    /** Code du motif d'écart (voir {@link MotifEcart}). */
    @Column(nullable = false)
    private Integer motifEcart;

    /** Précision libre sur le motif, facultative (exclue du toString : texte libre, hors logs). */
    @ToString.Exclude
    @Column(length = 1000)
    private String commentaire;

    /**
     * Retourne le libellé du motif d'écart, pour l'affichage.
     *
     * <p><b>Exemple :</b> motif 3 → « Distance » ; motif nul → « Inconnu ».</p>
     *
     * @return le libellé du motif, jamais {@code null}
     */
    public String getMotifLibelle() {
        return MotifEcart.libelle(motifEcart);
    }
}
