package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Entreprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Référentiel Spring Data JPA pour la gestion des entités {@link Entreprise}.
 * <p>
 * Fournit les opérations de persistance relatives aux entreprises : recherche
 * paginée, recherche par nom, vérification d'existence et filtrage des
 * entreprises actives.
 */
@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Integer> {

    /**
     * Récupère, de façon paginée, l'ensemble des entreprises triées par nom
     * dans l'ordre croissant.
     *
     * @param pageable les informations de pagination et de tri
     * @return une page d'entreprises triées par nom croissant
     *
     * <p><b>Exemple :</b> findAllByOrderByNomAsc(PageRequest.of(0, 10)) renvoie les 10 premières entreprises classées alphabétiquement par nom (« Acme » avant « Zenith »).</p>
     */
    Page<Entreprise> findAllByOrderByNomAsc(Pageable pageable);

    /**
     * Récupère toutes les entreprises dont le nom contient la chaîne indiquée,
     * sans tenir compte de la casse.
     *
     * @param nom le fragment de nom à rechercher
     * @return la liste des entreprises dont le nom contient le fragment
     *
     * <p><b>Exemple :</b> findAllByNomContainingIgnoreCase("acme") renvoie l'entreprise « Acme Corp » (recherche insensible à la casse).</p>
     */
    List<Entreprise> findAllByNomContainingIgnoreCase(String nom);

    /**
     * Récupère une entreprise à partir de son identifiant.
     *
     * @param id l'identifiant de l'entreprise recherchée
     * @return un {@link Optional} contenant l'entreprise si elle existe, vide sinon
     *
     * <p><b>Exemple :</b> findById(7) renvoie Optional.of(entreprise) si l'entreprise 7 existe, sinon Optional.empty().</p>
     */
    Optional<Entreprise> findById(Integer id);

    /**
     * Indique si une entreprise portant le nom donné existe déjà,
     * sans tenir compte de la casse.
     *
     * @param nom le nom de l'entreprise à vérifier
     * @return {@code true} si une entreprise avec ce nom existe, {@code false} sinon
     *
     * <p><b>Exemple :</b> existsByNomIgnoreCase("acme corp") renvoie true même si l'entreprise est enregistrée sous « Acme Corp ».</p>
     */
    Boolean existsByNomIgnoreCase(String nom);

    /**
     * Récupère toutes les entreprises actuellement actives.
     *
     * @return la liste des entreprises dont l'indicateur d'activité est vrai
     *
     * <p><b>Exemple :</b> findAllByEstActiveTrue() renvoie uniquement les entreprises dont estActive vaut true, en excluant les entreprises désactivées.</p>
     */
    List<Entreprise> findAllByEstActiveTrue();
}
