package com.mr486.gestojob.dto.api;

import com.mr486.gestojob.dto.OffreConsulteeForm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Demande d'API d'enregistrement d'une offre consultée puis écartée.
 *
 * <p><b>Exemple :</b> {@code {"intitule":"Tech lead IA","entreprise":"Foo SA",
 * "lien":"https://exemple.fr/o/7","motif":"DISTANCE","commentaire":"Lyon"}} ;
 * sans {@code dateConsultation}, la date du jour est retenue.</p>
 *
 * @param intitule          l'intitulé du poste
 * @param entreprise        le nom de l'entreprise
 * @param lien              le lien vers l'offre ({@code http(s)://}), facultatif
 * @param motif             le motif d'écart : {@code PROFIL}, {@code POSTE} ou {@code DISTANCE} (casse indifférente)
 * @param commentaire       la précision libre, facultative
 * @param dateConsultation  la date de lecture (ISO {@code yyyy-MM-dd}), facultative
 */
public record OffreConsulteeDemande(
        @NotBlank(message = "L'intitulé est obligatoire.")
        @Size(max = 255, message = "L'intitulé ne doit pas dépasser 255 caractères.") String intitule,
        @NotBlank(message = "L'entreprise est obligatoire.")
        @Size(max = 255, message = "L'entreprise ne doit pas dépasser 255 caractères.") String entreprise,
        @Size(max = 1000, message = "Le lien ne doit pas dépasser 1000 caractères.")
        @Pattern(regexp = OffreConsulteeForm.REGEX_LIEN, message = OffreConsulteeForm.MESSAGE_LIEN) String lien,
        @NotBlank(message = "Le motif est obligatoire.") String motif,
        @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères.") String commentaire,
        LocalDate dateConsultation) {
}
