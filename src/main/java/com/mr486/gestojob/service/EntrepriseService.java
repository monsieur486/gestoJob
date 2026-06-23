package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.dto.EntrepriseListe;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service métier de gestion des entreprises.
 * Gère la création, la mise à jour, l'activation/désactivation, la recherche
 * et le chargement groupé des entreprises.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;

    @Value("${gestojob.max-entreprises-par-page}")
    private int maxEntreprisesParPage;

    /**
     * Recopie les champs d'un formulaire vers une entité entreprise existante.
     *
     * <p><b>Exemple :</b> un formulaire de nom « Acme » et ville « Paris » écrase les champs nom et ville de l'entité cible avec ces valeurs.</p>
     *
     * @param form formulaire source
     * @param e    entité entreprise à mettre à jour
     */
    public static void convert(EntrepriseForm form, Entreprise e) {
        e.setNom(form.getNom());
        e.setEstActive(form.getEstActive());
        e.setAdresse1(form.getAdresse1());
        e.setAdresse2(form.getAdresse2());
        e.setCodePostal(form.getCodePostal());
        e.setVille(form.getVille());
    }

    /**
     * Crée et enregistre une nouvelle entreprise à partir du formulaire.
     *
     * <p><b>Exemple :</b> save(form avec nom « Acme ») retourne l'id généré ; si une entreprise « acme » existe déjà (casse ignorée), lève RuntimeException(« L'entreprise existe déjà »).</p>
     *
     * @param form formulaire de l'entreprise
     * @return l'identifiant de l'entreprise créée
     * @throws RuntimeException si une entreprise du même nom existe déjà
     */
    public int save(EntrepriseForm form) {
        if (entrepriseRepository.existsByNomIgnoreCase(form.getNom())) {
            log.warn("création d'entreprise refusée : « {} » existe déjà", form.getNom());
            throw new RuntimeException("L'entreprise existe déjà");
        }
        Entreprise entreprise = entrepriseRepository.save(form.entity(form));
        log.info("entreprise créée : « {} » (id {})", entreprise.getNom(), entreprise.getId());
        return entreprise.getId();
    }

    /**
     * Met à jour une entreprise existante à partir du formulaire.
     *
     * <p><b>Exemple :</b> update(3, form) met à jour l'entreprise 3 avec les valeurs du formulaire ; un id inexistant lève RuntimeException(« Entreprise introuvable avec id: 3 »).</p>
     *
     * @param entrepriseId identifiant de l'entreprise à modifier
     * @param form         formulaire contenant les nouvelles valeurs
     * @throws RuntimeException si l'entreprise est introuvable
     */
    public void update(Integer entrepriseId, EntrepriseForm form) {
        // On ne traduit en « introuvable » que l'absence d'entreprise : toute autre
        // erreur (échec de persistance, etc.) doit remonter sans être masquée.
        Entreprise e = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec id: " + entrepriseId));
        convert(form, e);
        entrepriseRepository.save(e);
        log.info("entreprise modifiée : « {} » (id {})", e.getNom(), entrepriseId);
    }

    /**
     * Construit le formulaire pré-rempli d'une entreprise (pour l'édition).
     *
     * <p><b>Exemple :</b> getForm(3) retourne un EntrepriseForm rempli avec les champs de l'entreprise 3 ; un id inexistant lève NoSuchElementException.</p>
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return le formulaire pré-rempli
     * @throws java.util.NoSuchElementException si l'entreprise est introuvable
     */
    public EntrepriseForm getForm(Integer entrepriseId) {
        return new EntrepriseForm(entrepriseRepository.findById(entrepriseId).orElseThrow());
    }

    /**
     * Récupère une entreprise par son identifiant.
     *
     * <p><b>Exemple :</b> getEntreprise(3) retourne l'entreprise 3 ; un id inexistant lève RuntimeException(« Entreprise introuvable avec id: 3 »).</p>
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return l'entreprise correspondante
     * @throws RuntimeException si l'entreprise est introuvable
     */
    public Entreprise getEntreprise(Integer entrepriseId) {
        return entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable avec id: " + entrepriseId));
    }

    /**
     * Charge en une seule requête toutes les entreprises demandées, indexées par id.
     * Évite le problème N+1 lors de la construction des listes d'annonces.
     *
     * <p><b>Exemple :</b> getEntreprisesByIds([1, 4]) retourne une map {1 -> entreprise 1, 4 -> entreprise 4} en une seule requête ; une collection nulle ou vide retourne une map vide.</p>
     *
     * @param ids identifiants des entreprises à charger (peut être nul ou vide)
     * @return une map des entreprises trouvées indexées par identifiant, vide si aucun id
     */
    public Map<Integer, Entreprise> getEntreprisesByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return entrepriseRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Entreprise::getId, Function.identity()));
    }

    /**
     * Désactive une entreprise (la marque comme inactive).
     *
     * <p><b>Exemple :</b> desactiveEntreprise(3) met estActive à false pour l'entreprise 3.</p>
     *
     * @param entrepriseId identifiant de l'entreprise
     * @throws java.util.NoSuchElementException si l'entreprise est introuvable
     */
    public void desactiveEntreprise(Integer entrepriseId) {
        Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
        e.setEstActive(false);
        entrepriseRepository.save(e);
        log.info("entreprise désactivée : « {} » (id {})", e.getNom(), entrepriseId);
    }

    /**
     * Active une entreprise (la marque comme active).
     *
     * <p><b>Exemple :</b> activeEntreprise(3) met estActive à true pour l'entreprise 3.</p>
     *
     * @param entrepriseId identifiant de l'entreprise
     * @throws java.util.NoSuchElementException si l'entreprise est introuvable
     */
    public void activeEntreprise(Integer entrepriseId) {
        Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
        e.setEstActive(true);
        entrepriseRepository.save(e);
        log.info("entreprise activée : « {} » (id {})", e.getNom(), entrepriseId);
    }

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
                .findAllByOrderByNomAsc(PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(EntrepriseService::toListe);
    }

    // Convertit une entité entreprise en ligne de liste (DTO d'affichage).
    private static EntrepriseListe toListe(Entreprise e) {
        return EntrepriseListe.builder()
                .id(e.getId())
                .nom(e.getNom())
                .estActive(e.getEstActive())
                .build();
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
                .findAllByNomContainingIgnoreCaseOrderByNomAsc(
                        nom, PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(EntrepriseService::toListe);
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
                .findAllByEstActiveTrueOrderByNomAsc(PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(EntrepriseService::toListe);
    }

    /**
     * Indique si une entreprise existe pour l'identifiant donné.
     *
     * <p><b>Exemple :</b> existe(3) retourne true si l'entreprise 3 est en base, false sinon.</p>
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@code true} si l'entreprise existe, {@code false} sinon
     */
    public Boolean existe(Integer entrepriseId) {
        return entrepriseRepository.existsById(entrepriseId);
    }

    /**
     * Retourne le nombre total d'entreprises enregistrées.
     *
     * <p><b>Exemple :</b> avec 5 entreprises en base, countAllEntreprises() retourne 5.</p>
     *
     * @return le nombre d'entreprises
     */
    public Integer countAllEntreprises() {
        return (int) entrepriseRepository.count();
    }
}
