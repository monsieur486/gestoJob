package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.OffreConsultee;
import com.mr486.gestojob.model.Titulaire;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Accès au journal des offres consultées. Toutes les lectures sont filtrées
 * par titulaire : ne jamais utiliser {@code findById}, {@code findAll},
 * {@code count} ni {@code deleteById} hérités.
 */
@Repository
public interface OffreConsulteeRepository extends JpaRepository<OffreConsultee, Long> {

    /**
     * Récupère, de façon paginée, les offres consultées d'un titulaire, les plus
     * récentes en tête (à date égale, la dernière saisie d'abord).
     *
     * <p><b>Exemple :</b> findAllByTitulaireOrderByDateConsultationDescIdDesc(LAURENT, PageRequest.of(0, 10))
     * renvoie les dix dernières offres écartées par Laurent.</p>
     *
     * @param titulaire le titulaire propriétaire
     * @param pageable  la page demandée
     * @return la page d'offres consultées
     */
    Page<OffreConsultee> findAllByTitulaireOrderByDateConsultationDescIdDesc(Titulaire titulaire, Pageable pageable);

    /**
     * Récupère une offre consultée par identifiant, seulement si elle appartient au titulaire.
     *
     * <p><b>Exemple :</b> findByIdAndTitulaire(42L, RENEE) est vide si l'entrée 42 appartient à LAURENT.</p>
     *
     * @param id        l'identifiant de l'entrée
     * @param titulaire le titulaire propriétaire
     * @return l'entrée, ou un optionnel vide si elle est inconnue ou appartient à l'autre titulaire
     */
    Optional<OffreConsultee> findByIdAndTitulaire(Long id, Titulaire titulaire);
}
