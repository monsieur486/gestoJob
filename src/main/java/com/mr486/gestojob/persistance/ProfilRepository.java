package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository des profils (un par titulaire, identifié par le titulaire).
 */
public interface ProfilRepository extends JpaRepository<Profil, Titulaire> {
}
