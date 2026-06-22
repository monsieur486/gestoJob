package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Annonce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Référentiel Spring Data JPA pour la gestion des entités {@link Annonce}.
 * <p>
 * Fournit les opérations de persistance (recherche, mise à jour de statut,
 * pagination et recherche plein texte) relatives aux annonces de candidature.
 */
@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    /**
     * Récupère toutes les annonces possédant le statut indiqué.
     *
     * @param i le statut de l'annonce recherché
     * @return la liste des annonces correspondant à ce statut
     *
     * <p><b>Exemple :</b> findAllByStatusAnnonce(1) renvoie les annonces en attente d'envoi.</p>
     */
    List<Annonce> findAllByStatusAnnonce(int i);

    /**
     * Met à jour le statut d'une annonce identifiée par son identifiant.
     *
     * @param annonceId     l'identifiant de l'annonce à modifier
     * @param statusAnnonce le nouveau statut à appliquer
     *
     * <p><b>Exemple :</b> updateStatusAnnonce(42L, 2) passe l'annonce 42 au statut archivé sans toucher à sa date d'envoi.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Annonce a set a.statusAnnonce = :statusAnnonce where a.id = :annonceId")
    void updateStatusAnnonce(@Param("annonceId") Long annonceId,
                             @Param("statusAnnonce") Integer statusAnnonce);

    /**
     * Met à jour à la fois le statut et la date d'envoi d'une annonce
     * identifiée par son identifiant.
     *
     * @param annonceId     l'identifiant de l'annonce à modifier
     * @param statusAnnonce le nouveau statut à appliquer
     * @param dateEnvoi     la nouvelle date d'envoi à enregistrer
     *
     * <p><b>Exemple :</b> updateStatusAnnonceEtDateEnvoi(42L, 1, now()) marque l'annonce 42 comme envoyée et enregistre l'instant de l'envoi.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Annonce a set a.statusAnnonce = :statusAnnonce, a.dateEnvoi = :dateEnvoi where a.id = :annonceId")
    void updateStatusAnnonceEtDateEnvoi(@Param("annonceId") Long annonceId,
                                        @Param("statusAnnonce") Integer statusAnnonce,
                                        @Param("dateEnvoi") OffsetDateTime dateEnvoi);

    /**
     * Récupère toutes les annonces d'une entreprise donnée, triées par statut
     * d'annonce dans l'ordre croissant.
     *
     * @param entrepriseId l'identifiant de l'entreprise concernée
     * @return la liste des annonces de l'entreprise triées par statut croissant
     *
     * <p><b>Exemple :</b> findAllByEntrepriseIdOrderByStatusAnnonceAsc(7) renvoie les annonces de l'entreprise 7, en plaçant d'abord celles au statut le plus bas (0, puis 1, puis 2).</p>
     */
    List<Annonce> findAllByEntrepriseIdOrderByStatusAnnonceAsc(Integer entrepriseId);

    /**
     * Récupère, de façon paginée, les annonces possédant le statut indiqué,
     * triées par date d'envoi décroissante.
     *
     * @param statusAnnonce le statut d'annonce recherché
     * @param pageable      les informations de pagination et de tri
     * @return une page d'annonces correspondant au statut, triées par date d'envoi décroissante
     *
     * <p><b>Exemple :</b> findAllByStatusAnnonceOrderByDateEnvoiDesc(1, PageRequest.of(0, 10)) renvoie les 10 annonces envoyées les plus récentes en premier.</p>
     */
    Page<Annonce> findAllByStatusAnnonceOrderByDateEnvoiDesc(Integer statusAnnonce, Pageable pageable);

    /**
     * Récupère, de façon paginée, l'ensemble des annonces triées par date
     * d'envoi décroissante, les annonces sans date d'envoi étant placées en fin.
     *
     * @param pageable les informations de pagination et de tri
     * @return une page de toutes les annonces triées par date d'envoi décroissante
     *
     * <p><b>Exemple :</b> findAllOrderByDateEnvoiDesc(PageRequest.of(0, 20)) renvoie toutes les annonces, la plus récemment envoyée en tête et les annonces jamais envoyées (dateEnvoi null) reléguées en fin de liste.</p>
     */
    @Query("""
            select a
            from Annonce a
            order by a.dateEnvoi desc nulls last
            """)
    Page<Annonce> findAllOrderByDateEnvoiDesc(Pageable pageable);

    /**
     * Recherche les annonces correspondant à un terme de recherche.
     * <p>
     * La recherche est insensible à la casse et porte sur le poste, la référence
     * de l'annonce, le nom de l'entreprise, l'email et le nom du contact. Les
     * annonces archivées (statut 2) ne sont incluses que si {@code includeArchives}
     * vaut {@code true}. Les résultats sont triés par date d'envoi décroissante,
     * les annonces sans date d'envoi étant placées en fin.
     *
     * @param q               le terme de recherche
     * @param includeArchives {@code true} pour inclure également les annonces archivées
     * @param pageable        les informations de pagination et de tri
     * @return une page d'annonces correspondant au terme de recherche
     *
     * <p><b>Exemple :</b> search("java", false, pageable) renvoie les annonces non archivées dont le poste, la référence, le nom de l'entreprise, l'email ou le nom du contact contient « java » (ex. poste « Développeur Java »), tandis que search("java", true, pageable) inclut aussi les annonces archivées (statut 2).</p>
     */
    @Query("""
            select a
            from Annonce a
            left join Entreprise e on e.id = a.entrepriseId
            left join Contact c on c.id = a.contactId
            where (
                lower(coalesce(a.poste, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(a.reference, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(e.nom, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.contact, '')) like lower(concat('%', :q, '%'))
            )
            and (:includeArchives = true or a.statusAnnonce = 2)
            order by a.dateEnvoi desc nulls last
            """)
    Page<Annonce> search(@Param("q") String q,
                         @Param("includeArchives") boolean includeArchives,
                         Pageable pageable);
}
