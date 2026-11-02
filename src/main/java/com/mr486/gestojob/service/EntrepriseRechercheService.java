package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.EntrepriseListe;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Service métier de recherche et de listing paginé des entreprises.
 * Regroupe les consultations en lecture (liste complète, recherche par nom,
 * entreprises actives), distinctes de la gestion (création, mise à jour,
 * activation) assurée par {@link EntrepriseService}.
 */
@Service
@RequiredArgsConstructor
public class EntrepriseRechercheService {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurCourant utilisateurCourant;

    @Value("${gestojob.max-entreprises-par-page}")
    private int maxEntreprisesParPage;

    /**
     * Retourne une page d'entreprises au format DTO, triées par nom croissant.
     * Les index de page négatifs sont ramenés à 0.
     *
     * <p><b>Exemple :</b> getAllListePage(-1) est traité comme la page 0 ; les entreprises sont triées par nom croissant (« Acme » avant « Beta »).</p>
     *
     * @param pageIndex index de la page (commençant à 0)
     * @return la page d'entreprises
     */
    public Page<EntrepriseListe> getAllListePage(int pageIndex) {
        int safePageIndex = Math.max(0, pageIndex);

        return entrepriseRepository
                .findAllByTitulaireOrderByNomAsc(utilisateurCourant.titulaire(),
                        PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(EntrepriseRechercheService::toListe);
    }

    /**
     * Recherche paginée des entreprises dont le nom contient la chaîne fournie
     * (insensible à la casse), triées par nom croissant. Les index de page
     * négatifs sont ramenés à 0.
     *
     * <p><b>Exemple :</b> rechercheEntrepriseParNomPage(« cm », 0) retourne la première page des entreprises dont le nom contient « cm » (ex. « Acme »).</p>
     *
     * @param nom       fragment de nom recherché
     * @param pageIndex index de la page (commençant à 0)
     * @return la page d'entreprises correspondantes au format DTO
     */
    public Page<EntrepriseListe> rechercheEntrepriseParNomPage(String nom, int pageIndex) {
        int safePageIndex = Math.max(0, pageIndex);
        return entrepriseRepository
                .findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc(
                        utilisateurCourant.titulaire(), nom, PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(EntrepriseRechercheService::toListe);
    }

    /**
     * Retourne une page d'entreprises actives au format DTO, triées par nom
     * croissant. Les index de page négatifs sont ramenés à 0.
     *
     * <p><b>Exemple :</b> rechercheEntrepriseActivePage(0) retourne la première page des entreprises dont estActive=true.</p>
     *
     * @param pageIndex index de la page (commençant à 0)
     * @return la page d'entreprises actives au format DTO
     */
    public Page<EntrepriseListe> rechercheEntrepriseActivePage(int pageIndex) {
        int safePageIndex = Math.max(0, pageIndex);
        return entrepriseRepository
                .findAllByTitulaireAndEstActiveTrueOrderByNomAsc(utilisateurCourant.titulaire(),
                        PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(EntrepriseRechercheService::toListe);
    }

    // Convertit une entité entreprise en ligne de liste (DTO d'affichage).
    private static EntrepriseListe toListe(Entreprise e) {
        return EntrepriseListe.builder()
                .id(e.getId())
                .nom(e.getNom())
                .estActive(e.getEstActive())
                .build();
    }
}
