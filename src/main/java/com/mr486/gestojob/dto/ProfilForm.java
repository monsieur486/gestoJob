package com.mr486.gestojob.dto;

import com.mr486.gestojob.model.Profil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de formulaire de modification du profil du titulaire courant : nom du
 * candidat, libellés d'affichage (64 caractères max) et postes par défaut
 * (255 caractères max) des trois lettres.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilForm {

    @NotBlank(message = "Le nom du candidat est obligatoire.")
    @Size(max = 255, message = "Le nom du candidat ne doit pas dépasser 255 caractères.")
    private String candidatNom;

    @NotBlank(message = "Le libellé de la lettre 1 est obligatoire.")
    @Size(max = 64, message = "Le libellé ne doit pas dépasser 64 caractères.")
    private String libelleGeneral;

    @NotBlank(message = "Le poste par défaut de la lettre 1 est obligatoire.")
    @Size(max = 255, message = "Le poste ne doit pas dépasser 255 caractères.")
    private String posteGeneral;

    @NotBlank(message = "Le libellé de la lettre 2 est obligatoire.")
    @Size(max = 64, message = "Le libellé ne doit pas dépasser 64 caractères.")
    private String libelleMicroservices;

    @NotBlank(message = "Le poste par défaut de la lettre 2 est obligatoire.")
    @Size(max = 255, message = "Le poste ne doit pas dépasser 255 caractères.")
    private String posteMicroservices;

    @NotBlank(message = "Le libellé de la lettre 3 est obligatoire.")
    @Size(max = 64, message = "Le libellé ne doit pas dépasser 64 caractères.")
    private String libelleIa;

    @NotBlank(message = "Le poste par défaut de la lettre 3 est obligatoire.")
    @Size(max = 255, message = "Le poste ne doit pas dépasser 255 caractères.")
    private String posteIa;

    /**
     * Construit un formulaire pré-rempli à partir d'un profil.
     *
     * <p><b>Exemple :</b> {@code ProfilForm.depuis(profil).getCandidatNom()} retourne
     * le nom du candidat du profil.</p>
     *
     * @param profil profil source
     * @return le formulaire correspondant
     */
    public static ProfilForm depuis(Profil profil) {
        return ProfilForm.builder()
                .candidatNom(profil.getCandidatNom())
                .libelleGeneral(profil.getLibelleGeneral())
                .posteGeneral(profil.getPosteGeneral())
                .libelleMicroservices(profil.getLibelleMicroservices())
                .posteMicroservices(profil.getPosteMicroservices())
                .libelleIa(profil.getLibelleIa())
                .posteIa(profil.getPosteIa())
                .build();
    }
}
