package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.ProfilForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.ProfilRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service du profil des titulaires : nom du candidat, libellés et postes par
 * défaut des trois lettres. Le profil du titulaire courant est modifiable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfilService {

    private final ProfilRepository profilRepository;
    private final UtilisateurCourant utilisateurCourant;

    /**
     * Retourne le profil d'un titulaire.
     *
     * <p><b>Exemple :</b> {@code getProfil(Titulaire.RENEE).getCandidatNom()} retourne « Renée ».</p>
     *
     * @param titulaire titulaire concerné
     * @return son profil
     * @throws MetierException si aucun profil n'existe pour ce titulaire
     */
    @Transactional(readOnly = true)
    public Profil getProfil(Titulaire titulaire) {
        return profilRepository.findById(titulaire)
                .orElseThrow(() -> new MetierException("Profil introuvable pour " + titulaire));
    }

    /**
     * Retourne le profil du titulaire courant (utilisateur connecté).
     *
     * <p><b>Exemple :</b> connecté en tant que Renée, retourne le profil RENEE.</p>
     *
     * @return le profil du titulaire courant
     * @throws MetierException si aucun profil n'existe
     */
    public Profil getProfilCourant() {
        return getProfil(utilisateurCourant.titulaire());
    }

    /**
     * Met à jour le profil du titulaire courant.
     *
     * <p><b>Exemple :</b> {@code mettreAJour(form)} avec {@code candidatNom="Renée D."}
     * enregistre ce nom sur le profil du titulaire connecté.</p>
     *
     * @param form valeurs saisies (déjà validées)
     * @throws MetierException si aucun profil n'existe pour le titulaire courant
     */
    @Transactional
    public void mettreAJour(ProfilForm form) {
        Profil profil = getProfilCourant();
        profil.setCandidatNom(form.getCandidatNom().trim());
        profil.setLibelleGeneral(form.getLibelleGeneral().trim());
        profil.setPosteGeneral(form.getPosteGeneral().trim());
        profil.setLibelleMicroservices(form.getLibelleMicroservices().trim());
        profil.setPosteMicroservices(form.getPosteMicroservices().trim());
        profil.setLibelleIa(form.getLibelleIa().trim());
        profil.setPosteIa(form.getPosteIa().trim());
        profilRepository.save(profil);
        log.info("profil de {} mis à jour", profil.getTitulaire());
    }

    /**
     * Retourne le libellé d'affichage d'une lettre pour ce profil.
     *
     * <p><b>Exemple :</b> pour RENEE, {@code libelleContenu(profil, CONTENU_IA)}
     * retourne « Lettre 3 ».</p>
     *
     * @param profil profil concerné
     * @param cle    clé de contenu ({@code CONTENU_*})
     * @return le libellé de la lettre
     * @throws IllegalArgumentException si la clé n'est pas une clé {@code CONTENU_*}
     */
    public String libelleContenu(Profil profil, CleModele cle) {
        return switch (cle) {
            case CONTENU_GENERAL -> profil.getLibelleGeneral();
            case CONTENU_MICROSERVICES -> profil.getLibelleMicroservices();
            case CONTENU_IA -> profil.getLibelleIa();
            default -> throw new IllegalArgumentException("Clé sans libellé de profil : " + cle);
        };
    }

    /**
     * Retourne l'intitulé de poste par défaut d'une lettre pour ce profil.
     *
     * <p><b>Exemple :</b> pour LAURENT, {@code posteParDefaut(profil, CONTENU_MICROSERVICES)}
     * retourne « de développeur Java orienté microservices ».</p>
     *
     * @param profil profil concerné
     * @param cle    clé de contenu ({@code CONTENU_*})
     * @return le poste par défaut
     * @throws IllegalArgumentException si la clé n'est pas une clé {@code CONTENU_*}
     */
    public String posteParDefaut(Profil profil, CleModele cle) {
        return switch (cle) {
            case CONTENU_GENERAL -> profil.getPosteGeneral();
            case CONTENU_MICROSERVICES -> profil.getPosteMicroservices();
            case CONTENU_IA -> profil.getPosteIa();
            default -> throw new IllegalArgumentException("Clé sans poste de profil : " + cle);
        };
    }
}
