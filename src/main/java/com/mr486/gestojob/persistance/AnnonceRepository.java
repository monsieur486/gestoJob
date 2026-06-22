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

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    List<Annonce> findAllByStatusAnnonce(int i);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Annonce a set a.statusAnnonce = :statusAnnonce where a.id = :annonceId")
    void updateStatusAnnonce(@Param("annonceId") Long annonceId,
                             @Param("statusAnnonce") Integer statusAnnonce);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Annonce a set a.statusAnnonce = :statusAnnonce, a.dateEnvoi = :dateEnvoi where a.id = :annonceId")
    void updateStatusAnnonceEtDateEnvoi(@Param("annonceId") Long annonceId,
                                        @Param("statusAnnonce") Integer statusAnnonce,
                                        @Param("dateEnvoi") OffsetDateTime dateEnvoi);

    List<Annonce> findAllByEntrepriseIdOrderByStatusAnnonceAsc(Integer entrepriseId);

    Page<Annonce> findAllByStatusAnnonceOrderByDateEnvoiDesc(Integer statusAnnonce, Pageable pageable);

    @Query("""
            select a
            from Annonce a
            order by a.dateEnvoi desc nulls last
            """)
    Page<Annonce> findAllOrderByDateEnvoiDesc(Pageable pageable);

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
