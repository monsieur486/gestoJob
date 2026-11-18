package com.mr486.gestojob.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Formulaire de saisie d'une offre consultée puis écartée.
 *
 * <p><b>Exemple :</b> date 18/11/2026, intitulé « Développeur Java senior »,
 * entreprise « Acme », lien {@code https://exemple.fr/offre/42}, motif 1 (profil).</p>
 */
@Data
@NoArgsConstructor
public class OffreConsulteeForm {

    /** Expression des liens acceptés : vide, ou http(s) sans espace (refuse {@code javascript:}). */
    public static final String REGEX_LIEN = "^$|^https?://\\S+$";

    /** Message d'erreur commun pour un lien refusé. */
    public static final String MESSAGE_LIEN = "Le lien doit commencer par http:// ou https://.";

    /** Date de lecture de l'offre. */
    @NotNull(message = "La date est obligatoire.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateConsultation;

    /** Intitulé du poste. */
    @NotBlank(message = "L'intitulé est obligatoire.")
    @Size(max = 255, message = "L'intitulé ne doit pas dépasser 255 caractères.")
    private String intitule;

    /** Nom de l'entreprise. */
    @NotBlank(message = "L'entreprise est obligatoire.")
    @Size(max = 255, message = "L'entreprise ne doit pas dépasser 255 caractères.")
    private String entreprise;

    /** Lien vers l'offre, facultatif. */
    @Size(max = 1000, message = "Le lien ne doit pas dépasser 1000 caractères.")
    @Pattern(regexp = REGEX_LIEN, message = MESSAGE_LIEN)
    private String lien;

    /** Code du motif d'écart (1 = profil, 2 = poste, 3 = distance). */
    @NotNull(message = "Le motif est obligatoire.")
    @Min(value = 1, message = "Motif inconnu.")
    @Max(value = 3, message = "Motif inconnu.")
    private Integer motifEcart;

    /** Précision libre, facultative. */
    @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères.")
    private String commentaire;
}
