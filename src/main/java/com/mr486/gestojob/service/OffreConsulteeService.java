package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.OffreConsulteeForm;
import com.mr486.gestojob.dto.api.OffreConsulteeDemande;
import com.mr486.gestojob.dto.api.OffreConsulteeReponse;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.MotifEcart;
import com.mr486.gestojob.model.OffreConsultee;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.OffreConsulteeRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Journal des offres consultées puis écartées : consultation paginée, ajout et
 * suppression, toujours pour le titulaire connecté.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OffreConsulteeService {

    private final OffreConsulteeRepository offreConsulteeRepository;
    private final UtilisateurCourant utilisateurCourant;

    @Value("${gestojob.max-annonces-par-page:7}")
    private int maxAnnoncesParPage;

    /**
     * Liste une page du journal du titulaire connecté, les plus récentes en tête.
     *
     * <p><b>Exemple :</b> {@code lister(0)} renvoie la première page ; un index négatif est ramené à 0.</p>
     *
     * @param pageIndex l'index de page (base 0)
     * @return la page d'offres consultées
     */
    @Transactional(readOnly = true)
    public Page<OffreConsultee> lister(int pageIndex) {
        return offreConsulteeRepository.findAllByTitulaireOrderByDateConsultationDescIdDesc(
                utilisateurCourant.titulaire(), PageRequest.of(Math.max(0, pageIndex), maxAnnoncesParPage));
    }

    /**
     * Enregistre une offre consultée pour le titulaire connecté. Les textes sont
     * débarrassés de leurs espaces de bord ; un lien ou un commentaire vide est
     * enregistré comme absent.
     *
     * <p><b>Exemple :</b> intitulé « {@code  Dév Java }» → « Dév Java », lien « {@code  } » → {@code null}.</p>
     *
     * @param form le formulaire validé
     * @return l'entrée enregistrée
     */
    @Transactional
    public OffreConsultee ajouter(OffreConsulteeForm form) {
        Titulaire titulaire = utilisateurCourant.titulaire();
        OffreConsultee entree = OffreConsultee.builder()
                .titulaire(titulaire)
                .dateConsultation(form.getDateConsultation())
                .intitule(nettoyer(form.getIntitule()))
                .entreprise(nettoyer(form.getEntreprise()))
                .lien(nettoyer(form.getLien()))
                .motifEcart(form.getMotifEcart())
                .commentaire(nettoyer(form.getCommentaire()))
                .build();
        OffreConsultee enregistree = offreConsulteeRepository.save(entree);
        log.info("Offre consultée enregistrée : id={} titulaire={}", enregistree.getId(), titulaire);
        return enregistree;
    }

    /**
     * Enregistre une offre consultée reçue par l'API Claude : le motif est donné
     * par son nom (casse indifférente) et la date, si absente, vaut aujourd'hui.
     *
     * <p><b>Exemple :</b> motif {@code "distance"} sans date → entrée au motif 3
     * datée du jour ; motif {@code "AUTRE"} → {@link MetierException} (400 JSON).</p>
     *
     * @param demande la demande validée
     * @return l'identifiant, la date et le nom du motif retenus
     * @throws MetierException si le motif est inconnu
     */
    @Transactional
    public OffreConsulteeReponse ajouterDepuisApi(OffreConsulteeDemande demande) {
        MotifEcart motif = MotifEcart.depuisNom(demande.motif())
                .orElseThrow(() -> new MetierException("Motif inconnu : attendu PROFIL, POSTE ou DISTANCE."));
        OffreConsulteeForm form = new OffreConsulteeForm();
        form.setDateConsultation(demande.dateConsultation() != null ? demande.dateConsultation() : LocalDate.now());
        form.setIntitule(demande.intitule());
        form.setEntreprise(demande.entreprise());
        form.setLien(demande.lien());
        form.setMotifEcart(motif.getCode());
        form.setCommentaire(demande.commentaire());
        OffreConsultee enregistree = ajouter(form);
        return new OffreConsulteeReponse(enregistree.getId(), enregistree.getDateConsultation(), motif.name());
    }

    /**
     * Supprime une entrée du journal du titulaire connecté.
     *
     * <p><b>Exemple :</b> {@code supprimer(42L)} lève {@link java.util.NoSuchElementException}
     * si l'entrée 42 est inconnue ou appartient à l'autre titulaire (→ 404).</p>
     *
     * @param id l'identifiant de l'entrée
     * @throws java.util.NoSuchElementException si l'entrée est introuvable pour ce titulaire
     */
    @Transactional
    public void supprimer(Long id) {
        Titulaire titulaire = utilisateurCourant.titulaire();
        OffreConsultee entree = offreConsulteeRepository.findByIdAndTitulaire(id, titulaire).orElseThrow();
        offreConsulteeRepository.delete(entree);
        log.info("Offre consultée supprimée : id={} titulaire={}", id, titulaire);
    }

    // Retire les espaces de bord ; une chaîne nulle ou blanche devient null.
    private static String nettoyer(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        return texte.strip();
    }
}
