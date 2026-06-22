package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.dto.EntrepriseListe;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;

    @Value("${gestojob.max-entreprises-par-page}")
    private int maxEntreprisesParPage;

    /**
     * Recopie les champs d'un formulaire vers une entité entreprise existante.
     *
     * @param form formulaire source
     * @param e    entité entreprise à mettre à jour
     *
     * <p><b>Exemple :</b> un formulaire de nom « Acme » et ville « Paris » écrase les champs nom et ville de l'entité cible avec ces valeurs.</p>
     */
    public static void Convert(EntrepriseForm form, Entreprise e) {
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
     * @param form formulaire de l'entreprise
     * @return l'identifiant de l'entreprise créée
     * @throws RuntimeException si une entreprise du même nom existe déjà
     *
     * <p><b>Exemple :</b> save(form avec nom « Acme ») retourne l'id généré ; si une entreprise « acme » existe déjà (casse ignorée), lève RuntimeException(« L'entreprise existe déjà »).</p>
     */
    public int save(EntrepriseForm form) {
        if (entrepriseRepository.existsByNomIgnoreCase(form.getNom())) {
            throw new RuntimeException("L'entreprise existe déjà");
        }
        Entreprise entreprise = entrepriseRepository.save(form.entity(form));
        return entreprise.getId();
    }

    /**
     * Met à jour une entreprise existante à partir du formulaire.
     *
     * @param entrepriseId identifiant de l'entreprise à modifier
     * @param form         formulaire contenant les nouvelles valeurs
     * @throws RuntimeException si l'entreprise est introuvable
     *
     * <p><b>Exemple :</b> update(3, form) met à jour l'entreprise 3 avec les valeurs du formulaire ; un id inexistant lève RuntimeException(« Entreprise introuvable avec id: 3 »).</p>
     */
    public void update(Integer entrepriseId, EntrepriseForm form) {
        try {
            Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
            Convert(form, e);
            entrepriseRepository.save(e);
        } catch (Exception ex) {
            throw new RuntimeException("Entreprise introuvable avec id: " + entrepriseId);
        }
    }

    /**
     * Construit le formulaire pré-rempli d'une entreprise (pour l'édition).
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return le formulaire pré-rempli
     * @throws java.util.NoSuchElementException si l'entreprise est introuvable
     *
     * <p><b>Exemple :</b> getForm(3) retourne un EntrepriseForm rempli avec les champs de l'entreprise 3 ; un id inexistant lève NoSuchElementException.</p>
     */
    public EntrepriseForm getForm(Integer entrepriseId) {
        return new EntrepriseForm(entrepriseRepository.findById(entrepriseId).orElseThrow());
    }

    /**
     * Récupère une entreprise par son identifiant.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return l'entreprise correspondante
     * @throws RuntimeException si l'entreprise est introuvable
     *
     * <p><b>Exemple :</b> getEntreprise(3) retourne l'entreprise 3 ; un id inexistant lève RuntimeException(« Entreprise introuvable avec id: 3 »).</p>
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
     * @param entrepriseId identifiant de l'entreprise
     * @throws java.util.NoSuchElementException si l'entreprise est introuvable
     *
     * <p><b>Exemple :</b> desactiveEntreprise(3) met estActive à false pour l'entreprise 3.</p>
     */
    public void desactiveEntreprise(Integer entrepriseId) {
        Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
        e.setEstActive(false);
        entrepriseRepository.save(e);
    }

    /**
     * Active une entreprise (la marque comme active).
     *
     * @param entrepriseId identifiant de l'entreprise
     * @throws java.util.NoSuchElementException si l'entreprise est introuvable
     *
     * <p><b>Exemple :</b> activeEntreprise(3) met estActive à true pour l'entreprise 3.</p>
     */
    public void activeEntreprise(Integer entrepriseId) {
        Entreprise e = entrepriseRepository.findById(entrepriseId).orElseThrow();
        e.setEstActive(true);
        entrepriseRepository.save(e);
    }

    /**
     * Retourne une page d'entreprises au format DTO, triées par nom croissant.
     * Les index de page négatifs sont ramenés à 0.
     *
     * @param pageIndex index de la page (commençant à 0)
     * @return la page d'entreprises
     *
     * <p><b>Exemple :</b> getAllListePage(-1) est traité comme la page 0 ; les entreprises sont triées par nom croissant (« Acme » avant « Beta »).</p>
     */
    public Page<EntrepriseListe> getAllListePage(int pageIndex) {
        int safePageIndex = Math.max(0, pageIndex);

        return entrepriseRepository
                .findAllByOrderByNomAsc(PageRequest.of(safePageIndex, maxEntreprisesParPage))
                .map(e -> EntrepriseListe.builder()
                        .id(e.getId())
                        .nom(e.getNom())
                        .estActive(e.getEstActive())
                        .build());
    }

    /**
     * Recherche les entreprises dont le nom contient la chaîne fournie (insensible à la casse).
     *
     * @param nom fragment de nom recherché
     * @return la liste des entreprises correspondantes au format DTO
     *
     * <p><b>Exemple :</b> rechercheEntrepriseParNom(« cm ») retourne l'entreprise « Acme » (recherche insensible à la casse sur un fragment du nom).</p>
     */
    public List<EntrepriseListe> rechercheEntrepriseParNom(String nom) {
        return entrepriseRepository.findAllByNomContainingIgnoreCase(nom)
                .stream()
                .map(e -> EntrepriseListe.builder()
                        .id(e.getId())
                        .nom(e.getNom())
                        .estActive(e.getEstActive())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Retourne la liste des entreprises actives au format DTO.
     *
     * @return la liste des entreprises actives
     *
     * <p><b>Exemple :</b> sur deux entreprises dont une seule a estActive=true, ne retourne que cette dernière.</p>
     */
    public List<EntrepriseListe> rechercheEntrepriseActive() {
        return entrepriseRepository.findAllByEstActiveTrue()
                .stream()
                .map(e -> EntrepriseListe.builder()
                        .id(e.getId())
                        .nom(e.getNom())
                        .estActive(e.getEstActive())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Indique si une entreprise existe pour l'identifiant donné.
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return {@code true} si l'entreprise existe, {@code false} sinon
     *
     * <p><b>Exemple :</b> existe(3) retourne true si l'entreprise 3 est en base, false sinon.</p>
     */
    public Boolean existe(Integer entrepriseId) {
        return entrepriseRepository.existsById(entrepriseId);
    }

    /**
     * Retourne le nombre total d'entreprises enregistrées.
     *
     * @return le nombre d'entreprises
     *
     * <p><b>Exemple :</b> avec 5 entreprises en base, countAllEntreprises() retourne 5.</p>
     */
    public Integer countAllEntreprises() {
        return (int) entrepriseRepository.count();
    }
}
