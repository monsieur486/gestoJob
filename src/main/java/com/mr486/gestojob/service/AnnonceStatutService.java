package com.mr486.gestojob.service;

import com.mr486.gestojob.model.StatutAnnonce;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier des transitions de statut d'une annonce (cycle de vie :
 * dépassée, refusée, acceptée). Regroupe les changements d'état, distincts de
 * la gestion générale des annonces assurée par {@link AnnonceService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnonceStatutService {

    private final AnnonceRepository annonceRepository;
    private final UtilisateurCourant utilisateurCourant;

    /**
     * Marque l'annonce comme dépassée / sans suite (statut 3).
     *
     * <p><b>Exemple :</b> setDepasse(7L) fait passer l'annonce 7 au statut 3 (dépassée).</p>
     *
     * @param annonceId identifiant de l'annonce
     * @throws java.util.NoSuchElementException si l'annonce est inconnue ou à un autre titulaire
     */
    @Transactional
    public void setDepasse(Long annonceId) {
        updateStatusAnnonce(annonceId, StatutAnnonce.DEPASSE.getCode());
        log.info("annonce marquée dépassée : {}", annonceId);
    }

    /**
     * Marque l'annonce comme refusée (statut 4).
     *
     * <p><b>Exemple :</b> setRefus(7L) fait passer l'annonce 7 au statut 4 (refusée).</p>
     *
     * @param annonceId identifiant de l'annonce
     * @throws java.util.NoSuchElementException si l'annonce est inconnue ou à un autre titulaire
     */
    @Transactional
    public void setRefus(Long annonceId) {
        updateStatusAnnonce(annonceId, StatutAnnonce.NEGATIF.getCode());
        log.info("annonce marquée refusée : {}", annonceId);
    }

    /**
     * Marque l'annonce comme acceptée (statut 5).
     *
     * <p><b>Exemple :</b> setAccepte(7L) fait passer l'annonce 7 au statut 5 (acceptée / positif).</p>
     *
     * @param annonceId identifiant de l'annonce
     * @throws java.util.NoSuchElementException si l'annonce est inconnue ou à un autre titulaire
     */
    @Transactional
    public void setAccepte(Long annonceId) {
        updateStatusAnnonce(annonceId, StatutAnnonce.POSITIF.getCode());
        log.info("annonce marquée acceptée (positif) : {}", annonceId);
    }

    // Met à jour le statut d'une annonce du titulaire courant ; 0 ligne modifiée
    // (id inconnu ou autre titulaire) lève NoSuchElementException.
    private void updateStatusAnnonce(Long annonceId, Integer statusAnnonce) {
        int modifiees = annonceRepository.updateStatusAnnonce(
                utilisateurCourant.titulaire(), annonceId, statusAnnonce);
        if (modifiees == 0) {
            throw new NoSuchElementException("Annonce introuvable : " + annonceId);
        }
    }
}
