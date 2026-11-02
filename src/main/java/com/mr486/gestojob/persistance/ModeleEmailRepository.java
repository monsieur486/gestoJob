package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.ModeleEmail;
import com.mr486.gestojob.model.Titulaire;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository des modèles d'email.
 */
public interface ModeleEmailRepository extends JpaRepository<ModeleEmail, Long> {

    /**
     * Recherche le modèle d'un titulaire par sa clé.
     *
     * <p><b>Exemple :</b> {@code findByTitulaireAndCle(RENEE, "CONTENU_IA")} retourne le
     * modèle IA de Renée s'il existe, sinon un Optional vide.</p>
     *
     * @param titulaire propriétaire du modèle
     * @param cle       clé stable du modèle
     * @return le modèle correspondant, s'il existe
     */
    Optional<ModeleEmail> findByTitulaireAndCle(Titulaire titulaire, String cle);
}
