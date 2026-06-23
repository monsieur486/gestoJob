package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Entreprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * <p><b>Exemple :</b> findAllByOrderByNomAsc(PageRequest.of(0, 10)) renvoie les 10 premières entreprises classées alphabétiquement par nom (« Acme » avant « Zenith »).</p>
     *
     * @param pageable les informations de pagination et de tri
     * @return une page d'entreprises triées par nom croissant
     */
    Page<Entreprise> findAllByOrderByNomAsc(Pageable pageable);

    /**
     * Récupère, de façon paginée, les entreprises dont le nom contient la chaîne
     * indiquée (insensible à la casse), triées par nom croissant.
     *
     * <p><b>Exemple :</b> findAllByNomContainingIgnoreCaseOrderByNomAsc("acme", PageRequest.of(0, 10)) renvoie la première page des entreprises dont le nom contient « acme ».</p>
     *
     * @param nom      le fragment de nom à rechercher
     * @param pageable les informations de pagination et de tri
     * @return une page d'entreprises dont le nom contient le fragment
     */
    Page<Entreprise> findAllByNomContainingIgnoreCaseOrderByNomAsc(String nom, Pageable pageable);

    /**
     * Récupère une entreprise à partir de son identifiant.
     *
     * <p><b>Exemple :</b> findById(7) renvoie Optional.of(entreprise) si l'entreprise 7 existe, sinon Optional.empty().</p>
     *
     * @param id l'identifiant de l'entreprise recherchée
     * @return un {@link Optional} contenant l'entreprise si elle existe, vide sinon
     */
    Optional<Entreprise> findById(Integer id);

    /**
     * Indique si une entreprise portant le nom donné existe déjà,
     * sans tenir compte de la casse.
     *
     * <p><b>Exemple :</b> existsByNomIgnoreCase("acme corp") renvoie true même si l'entreprise est enregistrée sous « Acme Corp ».</p>
     *
     * @param nom le nom de l'entreprise à vérifier
     * @return {@code true} si une entreprise avec ce nom existe, {@code false} sinon
     */
    Boolean existsByNomIgnoreCase(String nom);

    /**
     * Récupère, de façon paginée, les entreprises actuellement actives, triées
     * par nom croissant.
     *
     * <p><b>Exemple :</b> findAllByEstActiveTrueOrderByNomAsc(PageRequest.of(0, 10)) renvoie la première page des entreprises dont estActive vaut true.</p>
     *
     * @param pageable les informations de pagination et de tri
     * @return une page d'entreprises dont l'indicateur d'activité est vrai
     */
    Page<Entreprise> findAllByEstActiveTrueOrderByNomAsc(Pageable pageable);
}
