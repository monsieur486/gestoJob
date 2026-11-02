package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service métier de production du contenu <b>texte</b> d'une annonce (formule de
 * politesse et corps destiné au copier-coller). Pendant « texte brut » de
 * {@link AnnonceMailService} (qui produit la variante HTML des emails).
 */
@Service
@RequiredArgsConstructor
public class AnnonceTexteService {

    private final AnnonceRepository annonceRepository;
    private final ContactService contactService;
    private final ContenuService contenuService;
    private final LibelleService libelleService;
    private final UtilisateurCourant utilisateurCourant;

    /**
     * Retourne la formule de politesse à utiliser pour une annonce.
     * Renvoie une formule générique si l'annonce n'est associée à aucun contact.
     *
     * <p><b>Exemple :</b> pour une annonce sans contact, retourne « Madame, Monsieur, ».</p>
     *
     * @param annonceId identifiant de l'annonce
     * @return la formule de politesse, ou "Madame, Monsieur," par défaut
     */
    public String getMessageDePolitesse(Long annonceId) {
        return messageDePolitesse(chargerAnnonce(annonceId));
    }

    /**
     * Construit la version texte du contenu d'une annonce (libellé suivi du corps),
     * destinée par exemple au copier-coller.
     *
     * <p><b>Exemple :</b> getAnnonceTxtContenuById(7L) retourne le libellé de l'annonce suivi de deux sauts de ligne puis du corps texte (non échappé) intégrant la formule de politesse.</p>
     *
     * @param id identifiant de l'annonce
     * @return le contenu texte de l'annonce
     * @throws java.util.NoSuchElementException si l'annonce est introuvable
     */
    public String getAnnonceTxtContenuById(Long id) {
        Annonce annonce = chargerAnnonce(id);
        String result = libelleService.construitLibelle(annonce) + "\n\n";
        String messageDePolitesse = messageDePolitesse(annonce);

        result += contenuService.getTextContenu(
                annonce.getTitulaire(), annonce.getPoste(), annonce.getTypeContenu(), messageDePolitesse);

        return result;
    }

    // Charge une annonce par son identifiant (NoSuchElementException si absente).
    private Annonce chargerAnnonce(Long annonceId) {
        return annonceRepository.findByIdAndTitulaire(annonceId, utilisateurCourant.titulaire()).orElseThrow();
    }

    // Formule de politesse d'une annonce déjà chargée (générique si aucun contact),
    // sans relire l'annonce en base.
    private String messageDePolitesse(Annonce annonce) {
        Long contactId = annonce.getContactId();
        if (contactId == null) {
            return ApplicationConfiguration.SALUTATION_GENERIQUE;
        }
        return contactService.getContact(contactId).getMessageDePolitesse();
    }
}
