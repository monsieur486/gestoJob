package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.ModeleEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository des modèles d'email.
 */
public interface ModeleEmailRepository extends JpaRepository<ModeleEmail, Long> {

    /**
     * Recherche un modèle par sa clé.
     *
     * <p><b>Exemple :</b> {@code findByCle("CONTENU_IA")} retourne le modèle IA s'il
     * existe, sinon un Optional vide.</p>
     *
     * @param cle clé stable du modèle
     * @return le modèle correspondant, s'il existe
     */
    Optional<ModeleEmail> findByCle(String cle);
}
