package com.mr486.gestojob.controller.api;

import com.mr486.gestojob.dto.api.OffreConsulteeDemande;
import com.mr486.gestojob.dto.api.OffreConsulteeReponse;
import com.mr486.gestojob.service.OffreConsulteeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Claude : enregistrement d'une offre consultée puis écartée (skill
 * {@code gestion-annonce}). Ni lecture ni suppression par l'API ; les erreurs
 * sont rendues en JSON par {@link ApiErreurConseil}.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OffreConsulteeApiController {

    private final OffreConsulteeService offreConsulteeService;

    /**
     * Enregistre une offre consultée pour le titulaire de l'API.
     *
     * <p><b>Exemple :</b> {@code POST /api/offres-consultees} avec le motif {@code "AUTRE"}
     * répond 400 {@code {"erreur":"Motif inconnu : attendu PROFIL, POSTE ou DISTANCE."}}.</p>
     *
     * @param demande la demande validée
     * @return 201 avec l'identifiant, la date et le motif retenus
     */
    @PostMapping("/offres-consultees")
    public ResponseEntity<OffreConsulteeReponse> creer(@Valid @RequestBody OffreConsulteeDemande demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(offreConsulteeService.ajouterDepuisApi(demande));
    }
}
