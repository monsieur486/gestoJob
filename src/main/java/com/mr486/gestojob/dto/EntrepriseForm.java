package com.mr486.gestojob.dto;

import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.service.EntrepriseService;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de formulaire utilisé pour la création ou la modification d'une
 * entreprise. Il porte les contraintes de validation (nom, code postal et
 * ville obligatoires) et fait le lien avec l'entité
 * {@link com.mr486.gestojob.model.Entreprise}.
 */
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

    /**
     * Construit un formulaire d'entreprise pré-rempli à partir d'une entité
     * {@link Entreprise} existante, en recopiant ses différents champs.
     *
     * <p><b>Exemple :</b> pour une entité avec nom = « ACME » et ville = « Paris », le formulaire créé porte nom = « ACME » et ville = « Paris » (ainsi que estActive, adresse1, adresse2 et codePostal recopiés).</p>
     *
     * @param e l'entité entreprise source dont les valeurs sont reprises
     */
    public EntrepriseForm(Entreprise e) {
        this.nom = e.getNom();
        this.estActive = e.getEstActive();
        this.adresse1 = e.getAdresse1();
        this.adresse2 = e.getAdresse2();
        this.codePostal = e.getCodePostal();
        this.ville = e.getVille();
    }

    /**
     * Convertit un formulaire d'entreprise en une nouvelle entité
     * {@link Entreprise}, en déléguant la copie des champs au service
     * {@link EntrepriseService}.
     *
     * <p><b>Exemple :</b> pour un formulaire avec nom = « ACME » et ville = « Paris », retourne une nouvelle entité Entreprise dont nom = « ACME » et ville = « Paris ».</p>
     *
     * @param form le formulaire source contenant les données saisies
     * @return une nouvelle entité {@link Entreprise} alimentée à partir du formulaire
     */
    public Entreprise entity(EntrepriseForm form) {
        Entreprise e = new Entreprise();
        EntrepriseService.convert(form, e);
        return e;
    }
}
