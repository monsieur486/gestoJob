package com.mr486.gestojob.controller.api;

import com.mr486.gestojob.dto.api.CandidatureDemande;
import com.mr486.gestojob.dto.api.CandidatureReponse;
import com.mr486.gestojob.dto.api.EntrepriseApi;
import com.mr486.gestojob.dto.api.ModeleLettreApi;
import com.mr486.gestojob.service.CandidatureApiService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Points d'entrée JSON de l'API Claude ({@code /api/**}) : modèle de lettre, recherche
 * d'entreprises et création d'une candidature.
 *
 * <p>L'authentification par jeton est assurée en amont par la chaîne de sécurité de l'API ; le
 * titulaire n'est jamais lu dans la requête. Les erreurs sont rendues par {@link ApiErreurConseil}.</p>
 *
 * <p><b>Exemple :</b> {@code GET /api/entreprises?nom=acme} liste les entreprises de LAURENT dont le
 * nom contient « acme ».</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CandidatureApiController {

    private final CandidatureApiService candidatureApiService;

    /**
     * Renvoie le modèle de lettre à personnaliser.
     *
     * <p><b>Exemple :</b> {@code GET /api/modele-lettre}.</p>
     *
     * @return le modèle de lettre et ses variables
     */
    @GetMapping("/modele-lettre")
    public ModeleLettreApi modeleLettre() {
        return candidatureApiService.modeleLettre();
    }

    /**
     * Recherche les entreprises par nom.
     *
     * <p><b>Exemple :</b> {@code GET /api/entreprises?nom=acme}.</p>
     *
     * @param nom fragment du nom recherché ; obligatoire (une erreur 400 sinon)
     * @return les entreprises trouvées
     */
    @GetMapping("/entreprises")
    public List<EntrepriseApi> entreprises(@RequestParam(required = false) String nom) {
        return candidatureApiService.rechercherEntreprises(nom);
    }

    /**
     * Crée une candidature (entreprise, contact et annonce) sans jamais envoyer de mail.
     *
     * <p><b>Exemple :</b> {@code POST /api/candidatures} avec une entreprise, une annonce et une
     * lettre HTML répond {@code 201} et les identifiants créés.</p>
     *
     * @param demande la candidature à créer, validée
     * @return {@code 201} et le compte rendu de création
     */
    @PostMapping("/candidatures")
    public ResponseEntity<CandidatureReponse> creer(@Valid @RequestBody CandidatureDemande demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(candidatureApiService.creer(demande));
    }
}
