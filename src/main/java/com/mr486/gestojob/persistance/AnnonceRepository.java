package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Titulaire;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Référentiel Spring Data JPA pour la gestion des entités {@link Annonce}.
 *
 * <p>Fournit les opérations de persistance (recherche, mise à jour de statut,
 * pagination et recherche plein texte) relatives aux annonces de candidature.
 */
@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    /**
     * Récupère une annonce par son identifiant, uniquement si elle appartient au titulaire.
     *
     * <p><b>Exemple :</b> findByIdAndTitulaire(7L, RENEE) renvoie l'annonce 7 si elle est à Renée, sinon un Optional vide (même réponse qu'un id inexistant).</p>
     *
     * @param id        l'identifiant de l'annonce
     * @param titulaire le titulaire propriétaire attendu
     * @return l'annonce, ou un Optional vide
     */
    Optional<Annonce> findByIdAndTitulaire(Long id, Titulaire titulaire);

    /**
     * Compte les annonces d'un titulaire.
     *
     * <p><b>Exemple :</b> countByTitulaire(RENEE) renvoie le nombre d'annonces de Renée.</p>
     *
     * @param titulaire le titulaire concerné
     * @return le nombre d'annonces
     */
    long countByTitulaire(Titulaire titulaire);

    /**
     * Indique si au moins une annonce du titulaire est rattachée au contact donné.
     *
     * <p><b>Exemple :</b> existsByContactIdAndTitulaire(5L, RENEE) renvoie true si une annonce de Renée référence le contact 5, false sinon.</p>
     *
     * @param contactId l'identifiant du contact concerné
     * @param titulaire le titulaire concerné
     * @return {@code true} si une annonce référence ce contact, {@code false} sinon
     */
    boolean existsByContactIdAndTitulaire(Long contactId, Titulaire titulaire);

    /**
     * Récupère toutes les annonces d'un titulaire possédant le statut indiqué.
     *
     * <p><b>Exemple :</b> findAllByTitulaireAndStatusAnnonce(RENEE, 1) renvoie les annonces de Renée en attente d'envoi.</p>
     *
     * @param titulaire le titulaire concerné
     * @param i le statut de l'annonce recherché
     * @return la liste des annonces correspondant à ce statut
     */
    List<Annonce> findAllByTitulaireAndStatusAnnonce(Titulaire titulaire, int i);

    /**
     * Met à jour le statut d'une annonce identifiée par son identifiant.
     *
     * <p><b>Exemple :</b> updateStatusAnnonce(RENEE, 42L, 2) passe l'annonce 42 au statut archivé sans toucher à sa date d'envoi.</p>
     *
     * @param titulaire     le titulaire propriétaire de l'annonce
     * @param annonceId     l'identifiant de l'annonce à modifier
     * @param statusAnnonce le nouveau statut à appliquer
     * @return le nombre de lignes modifiées (0 si l'annonce est inconnue ou à un autre titulaire)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Annonce a set a.statusAnnonce = :statusAnnonce "
            + "where a.id = :annonceId and a.titulaire = :titulaire")
    int updateStatusAnnonce(@Param("titulaire") Titulaire titulaire,
                            @Param("annonceId") Long annonceId,
                            @Param("statusAnnonce") Integer statusAnnonce);

    /**
     * Met à jour à la fois le statut et la date d'envoi d'une annonce
     * identifiée par son identifiant.
     *
     * <p><b>Exemple :</b> updateStatusAnnonceEtDateEnvoi(RENEE, 42L, 1, now()) marque l'annonce 42 comme envoyée et enregistre l'instant de l'envoi.</p>
     *
     * @param titulaire     le titulaire propriétaire de l'annonce
     * @param annonceId     l'identifiant de l'annonce à modifier
     * @param statusAnnonce le nouveau statut à appliquer
     * @param dateEnvoi     la nouvelle date d'envoi à enregistrer
     * @return le nombre de lignes modifiées (0 si l'annonce est inconnue ou à un autre titulaire)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Annonce a set a.statusAnnonce = :statusAnnonce, a.dateEnvoi = :dateEnvoi "
            + "where a.id = :annonceId and a.titulaire = :titulaire")
    int updateStatusAnnonceEtDateEnvoi(@Param("titulaire") Titulaire titulaire,
                                       @Param("annonceId") Long annonceId,
                                       @Param("statusAnnonce") Integer statusAnnonce,
                                       @Param("dateEnvoi") OffsetDateTime dateEnvoi);

    /**
     * Récupère toutes les annonces d'un titulaire pour une entreprise donnée, triées par statut
     * d'annonce dans l'ordre croissant.
     *
     * <p><b>Exemple :</b> findAllByTitulaireAndEntrepriseIdOrderByStatusAnnonceAsc(RENEE, 7) renvoie les annonces de l'entreprise 7, en plaçant d'abord celles au statut le plus bas (0, puis 1, puis 2).</p>
     *
     * @param titulaire    le titulaire concerné
     * @param entrepriseId l'identifiant de l'entreprise concernée
     * @return la liste des annonces de l'entreprise triées par statut croissant
     */
    List<Annonce> findAllByTitulaireAndEntrepriseIdOrderByStatusAnnonceAsc(Titulaire titulaire, Integer entrepriseId);

    /**
     * Récupère, de façon paginée, les annonces possédant le statut indiqué,
     * triées par date d'envoi décroissante.
     *
     * <p><b>Exemple :</b> findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(RENEE, 1, PageRequest.of(0, 10)) renvoie les 10 annonces envoyées les plus récentes en premier.</p>
     *
     * @param titulaire     le titulaire concerné
     * @param statusAnnonce le statut d'annonce recherché
     * @param pageable      les informations de pagination et de tri
     * @return une page d'annonces correspondant au statut, triées par date d'envoi décroissante
     */
    Page<Annonce> findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(Titulaire titulaire,
                                                                         Integer statusAnnonce,
                                                                         Pageable pageable);

    /**
     * Récupère, de façon paginée, l'ensemble des annonces triées par date
     * d'envoi décroissante, les annonces sans date d'envoi étant placées en fin.
     *
     * <p><b>Exemple :</b> findAllOrderByDateEnvoiDesc(RENEE, PageRequest.of(0, 20)) renvoie toutes les annonces, la plus récemment envoyée en tête et les annonces jamais envoyées (dateEnvoi null) reléguées en fin de liste.</p>
     *
     * @param titulaire le titulaire concerné
     * @param pageable les informations de pagination et de tri
     * @return une page de toutes les annonces triées par date d'envoi décroissante
     */
    @Query("""
            select a
            from Annonce a
            where a.titulaire = :titulaire
            order by a.dateEnvoi desc nulls last
            """)
    Page<Annonce> findAllOrderByDateEnvoiDesc(@Param("titulaire") Titulaire titulaire, Pageable pageable);

    /**
     * Récupère, de façon paginée, toutes les annonces dont le statut diffère de
     * celui fourni, triées par date d'envoi décroissante (annonces sans date en fin).
     *
     * <p><b>Exemple :</b> findAllByStatusAnnonceNotOrderByDateEnvoiDesc(RENEE, 6, PageRequest.of(0, 20)) renvoie toutes les annonces sauf les archivées (statut 6), y compris celles sans statut, la plus récemment envoyée en tête.</p>
     *
     * @param titulaire le titulaire concerné
     * @param status   le statut à exclure des résultats
     * @param pageable les informations de pagination et de tri
     * @return une page d'annonces dont le statut diffère de {@code status} (les statuts nuls sont inclus)
     */
    @Query("""
            select a
            from Annonce a
            where a.titulaire = :titulaire
            and (a.statusAnnonce <> :status or a.statusAnnonce is null)
            order by a.dateEnvoi desc nulls last
            """)
    Page<Annonce> findAllByStatusAnnonceNotOrderByDateEnvoiDesc(@Param("titulaire") Titulaire titulaire,
                                                                @Param("status") Integer status,
                                                                Pageable pageable);

    /**
     * Recherche les annonces correspondant à un terme de recherche.
     *
     * <p>La recherche est insensible à la casse et porte sur le poste, la référence
     * de l'annonce, le nom de l'entreprise, l'email et le nom du contact. Les
     * annonces archivées (statut 6) ne sont incluses que si {@code includeArchives}
     * vaut {@code true} ; sinon tous les autres statuts sont retournés. Les
     * résultats sont triés par date d'envoi décroissante, les annonces sans date
     * d'envoi étant placées en fin.
     *
     * <p><b>Exemple :</b> search(RENEE, "java", false, pageable) renvoie les annonces non archivées dont le poste, la référence, le nom de l'entreprise, l'email ou le nom du contact contient « java » (ex. poste « Développeur Java »), tandis que search(RENEE, "java", true, pageable) inclut aussi les annonces archivées (statut 6).</p>
     *
     * @param titulaire       le titulaire concerné (annonces, entreprises et contacts)
     * @param q               le terme de recherche
     * @param includeArchives {@code true} pour inclure également les annonces archivées
     * @param pageable        les informations de pagination et de tri
     * @return une page d'annonces correspondant au terme de recherche
     */
    @Query("""
            select a
            from Annonce a
            left join Entreprise e on e.id = a.entrepriseId and e.titulaire = :titulaire
            left join Contact c on c.id = a.contactId and c.titulaire = :titulaire
            where a.titulaire = :titulaire
            and (
                lower(coalesce(a.poste, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(a.reference, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(e.nom, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.nom, '')) like lower(concat('%', :q, '%'))
            )
            and (:includeArchives = true or a.statusAnnonce <> 6 or a.statusAnnonce is null)
            order by a.dateEnvoi desc nulls last
            """)
    Page<Annonce> search(@Param("titulaire") Titulaire titulaire,
                         @Param("q") String q,
                         @Param("includeArchives") boolean includeArchives,
                         Pageable pageable);
}
