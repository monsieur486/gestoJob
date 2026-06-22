package com.mr486.gestojob.dto;

import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.service.EntrepriseService;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseForm {

    @NotBlank(message = "Le nom est obligatoire.")
    private String nom;

    private Boolean estActive = true;

    private String adresse1;
    private String adresse2;

    @NotBlank(message = "Le code postal est obligatoire.")
    private String codePostal = "67000";

    @NotBlank(message = "La ville est obligatoire.")
    private String ville = "Strasbourg";

    public EntrepriseForm(Entreprise e) {
        this.nom = e.getNom();
        this.estActive = e.getEstActive();
        this.adresse1 = e.getAdresse1();
        this.adresse2 = e.getAdresse2();
        this.codePostal = e.getCodePostal();
        this.ville = e.getVille();
    }

    public Entreprise entity(EntrepriseForm form) {
        Entreprise e = new Entreprise();
        EntrepriseService.Convert(form, e);
        return e;
    }
}
