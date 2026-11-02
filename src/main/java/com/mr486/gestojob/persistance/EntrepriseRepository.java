package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.Titulaire;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Référentiel Spring Data JPA pour la gestion des entités {@link Entreprise}.
 *
 * <p>Toutes les requêtes déclarées ici sont filtrées par {@link Titulaire} : chaque
 * titulaire ne voit que ses propres entreprises.
 */
@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Integer> {

    /**
     * Récupère, de façon paginée, les entreprises d'un titulaire triées par nom croissant.
     *
     * <p><b>Exemple :</b> findAllByTitulaireOrderByNomAsc(RENEE, PageRequest.of(0, 10)) renvoie les 10 premières entreprises de Renée classées par nom.</p>
     *
     * @param titulaire le titulaire propriétaire
     * @param pageable  les informations de pagination et de tri
     * @return une page d'entreprises triées par nom croissant
     */
    Page<Entreprise> findAllByTitulaireOrderByNomAsc(Titulaire titulaire, Pageable pageable);

    /**
     * Récupère, de façon paginée, les entreprises d'un titulaire dont le nom contient
     * la chaîne indiquée (insensible à la casse), triées par nom croissant.
     *
     * <p><b>Exemple :</b> findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc(RENEE, "acme", PageRequest.of(0, 10)) renvoie la première page des entreprises de Renée dont le nom contient « acme ».</p>
     *
     * @param titulaire le titulaire propriétaire
     * @param nom       le fragment de nom à rechercher
     * @param pageable  les informations de pagination et de tri
     * @return une page d'entreprises dont le nom contient le fragment
     */
    Page<Entreprise> findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc(
            Titulaire titulaire, String nom, Pageable pageable);

    /**
     * Récupère une entreprise à partir de son identifiant, si elle appartient au titulaire.
     *
     * <p><b>Exemple :</b> findByIdAndTitulaire(7, RENEE) renvoie l'entreprise 7 si elle est à Renée, Optional.empty() sinon (inexistante ou à un autre titulaire).</p>
     *
     * @param id        l'identifiant de l'entreprise recherchée
     * @param titulaire le titulaire propriétaire
     * @return un {@link Optional} contenant l'entreprise si elle existe pour ce titulaire, vide sinon
     */
    Optional<Entreprise> findByIdAndTitulaire(Integer id, Titulaire titulaire);

    /**
     * Indique si le titulaire possède déjà une entreprise du nom donné, casse ignorée.
     *
     * <p><b>Exemple :</b> existsByTitulaireAndNomIgnoreCase(RENEE, "acme corp") renvoie true si Renée a une entreprise « Acme Corp ».</p>
     *
     * @param titulaire le titulaire propriétaire
     * @param nom       le nom de l'entreprise à vérifier
     * @return {@code true} si une entreprise avec ce nom existe pour ce titulaire, {@code false} sinon
     */
    Boolean existsByTitulaireAndNomIgnoreCase(Titulaire titulaire, String nom);

    /**
     * Indique si l'entreprise d'identifiant donné appartient au titulaire.
     *
     * <p><b>Exemple :</b> existsByIdAndTitulaire(7, RENEE) renvoie false si l'entreprise 7 est à Laurent.</p>
     *
     * @param id        l'identifiant de l'entreprise
     * @param titulaire le titulaire propriétaire
     * @return {@code true} si l'entreprise existe pour ce titulaire, {@code false} sinon
     */
    boolean existsByIdAndTitulaire(Integer id, Titulaire titulaire);

    /**
     * Récupère, de façon paginée, les entreprises actives d'un titulaire, triées par nom croissant.
     *
     * <p><b>Exemple :</b> findAllByTitulaireAndEstActiveTrueOrderByNomAsc(RENEE, PageRequest.of(0, 10)) renvoie la première page des entreprises actives de Renée.</p>
     *
     * @param titulaire le titulaire propriétaire
     * @param pageable  les informations de pagination et de tri
     * @return une page d'entreprises dont l'indicateur d'activité est vrai
     */
    Page<Entreprise> findAllByTitulaireAndEstActiveTrueOrderByNomAsc(Titulaire titulaire, Pageable pageable);

    /**
     * Compte les entreprises d'un titulaire.
     *
     * <p><b>Exemple :</b> countByTitulaire(RENEE) renvoie 5 si Renée a 5 entreprises.</p>
     *
     * @param titulaire le titulaire propriétaire
     * @return le nombre d'entreprises du titulaire
     */
    long countByTitulaire(Titulaire titulaire);

    /**
     * Charge, parmi les identifiants donnés, les entreprises appartenant au titulaire.
     *
     * <p><b>Exemple :</b> findAllByIdInAndTitulaire([1, 4], RENEE) ne renvoie que celles des entreprises 1 et 4 qui sont à Renée.</p>
     *
     * @param ids       les identifiants recherchés
     * @param titulaire le titulaire propriétaire
     * @return la liste des entreprises trouvées
     */
    List<Entreprise> findAllByIdInAndTitulaire(Collection<Integer> ids, Titulaire titulaire);
}
