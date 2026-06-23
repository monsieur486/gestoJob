package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Référentiel Spring Data JPA pour la gestion des entités {@link Contact}.
 * <p>
 * Fournit les opérations de persistance relatives aux contacts rattachés
 * aux entreprises.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    /**
     * Récupère tous les contacts rattachés à une entreprise donnée.
     *
     * <p><b>Exemple :</b> findAllByEntrepriseId(7) renvoie tous les contacts rattachés à l'entreprise 7, et une liste vide si elle n'en possède aucun.</p>
     *
     * @param id l'identifiant de l'entreprise concernée
     * @return la liste des contacts de l'entreprise
     */
    List<Contact> findAllByEntrepriseId(Integer id);

    /**
     * Indique si un contact possédant l'email donné existe déjà pour une
     * entreprise, sans tenir compte de la casse.
     *
     * <p><b>Exemple :</b> existsByEntrepriseIdAndEmailIgnoreCase(7, "a@b.fr") renvoie true si l'entreprise 7 a déjà un contact dont l'email vaut « a@b.fr » (ou « A@B.FR »), false sinon.</p>
     *
     * @param entrepriseId l'identifiant de l'entreprise concernée
     * @param email        l'email à vérifier
     * @return {@code true} si un tel contact existe déjà, {@code false} sinon
     */
    boolean existsByEntrepriseIdAndEmailIgnoreCase(Integer entrepriseId, String email);
}
