package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Titulaire;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Référentiel Spring Data JPA pour la gestion des entités {@link Contact}.
 *
 * <p>Toutes les requêtes déclarées ici sont filtrées par {@link Titulaire} : chaque
 * titulaire ne voit que ses propres contacts.
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    /**
     * Récupère un contact par son identifiant, s'il appartient au titulaire.
     *
     * <p><b>Exemple :</b> findByIdAndTitulaire(9L, RENEE) renvoie le contact 9 s'il est à Renée, Optional.empty() sinon.</p>
     *
     * @param id        l'identifiant du contact
     * @param titulaire le titulaire propriétaire
     * @return un {@link Optional} contenant le contact s'il existe pour ce titulaire, vide sinon
     */
    Optional<Contact> findByIdAndTitulaire(Long id, Titulaire titulaire);

    /**
     * Récupère tous les contacts d'un titulaire rattachés à une entreprise donnée.
     *
     * <p><b>Exemple :</b> findAllByEntrepriseIdAndTitulaire(7, RENEE) renvoie les contacts de Renée pour l'entreprise 7, et une liste vide s'il n'y en a aucun.</p>
     *
     * @param id        l'identifiant de l'entreprise concernée
     * @param titulaire le titulaire propriétaire
     * @return la liste des contacts de l'entreprise pour ce titulaire
     */
    List<Contact> findAllByEntrepriseIdAndTitulaire(Integer id, Titulaire titulaire);

    /**
     * Indique si un contact possédant l'email donné existe déjà pour une
     * entreprise du titulaire, sans tenir compte de la casse.
     *
     * <p><b>Exemple :</b> existsByTitulaireAndEntrepriseIdAndEmailIgnoreCase(RENEE, 7, "a@b.fr") renvoie true si Renée a déjà, pour l'entreprise 7, un contact dont l'email vaut « a@b.fr » (ou « A@B.FR »).</p>
     *
     * @param titulaire    le titulaire propriétaire
     * @param entrepriseId l'identifiant de l'entreprise concernée
     * @param email        l'email à vérifier
     * @return {@code true} si un tel contact existe déjà, {@code false} sinon
     */
    boolean existsByTitulaireAndEntrepriseIdAndEmailIgnoreCase(Titulaire titulaire, Integer entrepriseId, String email);

    /**
     * Charge, parmi les identifiants donnés, les contacts appartenant au titulaire.
     *
     * <p><b>Exemple :</b> findAllByIdInAndTitulaire([2L, 5L], RENEE) ne renvoie que ceux des contacts 2 et 5 qui sont à Renée.</p>
     *
     * @param ids       les identifiants recherchés
     * @param titulaire le titulaire propriétaire
     * @return la liste des contacts trouvés
     */
    List<Contact> findAllByIdInAndTitulaire(Collection<Long> ids, Titulaire titulaire);
}
