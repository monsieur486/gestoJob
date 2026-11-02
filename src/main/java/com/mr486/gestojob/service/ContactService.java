package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.persistance.ContactRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier de gestion des contacts associés aux entreprises.
 * Gère la création, la consultation et le chargement groupé des contacts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;
    private final EntrepriseService entrepriseService;
    private final AnnonceRepository annonceRepository;
    private final UtilisateurCourant utilisateurCourant;

    /**
     * Supprime un contact, sauf s'il est encore rattaché à des annonces.
     *
     * <p><b>Exemple :</b> deleteContact(9L) supprime le contact 9 s'il n'est lié à aucune annonce ; sinon lève MetierException(« … rattaché à des annonces … »).</p>
     *
     * @param contactId identifiant du contact à supprimer
     * @throws MetierException si le contact est encore référencé par une annonce
     */
    @Transactional
    public void deleteContact(Long contactId) {
        Contact contact = getContact(contactId);
        if (annonceRepository.existsByContactIdAndTitulaire(contactId, utilisateurCourant.titulaire())) {
            log.warn("suppression refusée : le contact {} est rattaché à des annonces", contactId);
            throw new MetierException("Ce contact est rattaché à des annonces ; supprimez-les d'abord.");
        }
        contactRepository.delete(contact);
        log.info("contact supprimé : {}", contactId);
    }

    /**
     * Crée et enregistre un contact pour une entreprise.
     *
     * <p><b>Exemple :</b> un formulaire avec formuleDePolitesse=1 et email vide lève MetierException(« Veuillez renseigner un email »).</p>
     *
     * @param contactForm  formulaire du contact
     * @param entrepriseId identifiant de l'entreprise rattachée
     * @throws MetierException si l'entreprise est introuvable, si une formule de
     *                         politesse est choisie sans email renseigné, ou si un
     *                         contact avec le même email existe déjà pour l'entreprise
     */
    @Transactional
    public void saveContact(ContactForm contactForm, int entrepriseId) {
        validerEntrepriseExistante(entrepriseId);
        validerEmailRequisSiPolitesse(contactForm, entrepriseId);
        validerUniciteEmail(contactForm, entrepriseId);

        Contact contact = new Contact();
        contact.setTitulaire(utilisateurCourant.titulaire());
        contact.setEntrepriseId(entrepriseId);
        contact.setFormuleDePolitesse(contactForm.getFormuleDePolitesse());
        contact.setEmail(contactForm.getEmail());
        contact.setNom(contactForm.getNom());
        contactRepository.save(contact);
        log.info("contact créé pour l'entreprise {}", entrepriseId);
    }

    // Refuse la création si l'entreprise de rattachement n'existe pas.
    private void validerEntrepriseExistante(int entrepriseId) {
        if (!entrepriseService.existe(entrepriseId)) {
            log.warn("création de contact refusée : entreprise {} introuvable", entrepriseId);
            throw new MetierException("Entreprise introuvable avec id: " + entrepriseId);
        }
    }

    // Exige un email dès qu'une formule de politesse (> 0) est choisie : sans
    // adresse, l'email de candidature ne pourrait pas être envoyé.
    private void validerEmailRequisSiPolitesse(ContactForm contactForm, int entrepriseId) {
        Integer formuleDePolitesse = contactForm.getFormuleDePolitesse();
        String email = contactForm.getEmail();
        if (formuleDePolitesse != null && formuleDePolitesse > 0 && (email == null || email.isBlank())) {
            log.warn("création de contact refusée : formule de politesse sans email (entreprise {})", entrepriseId);
            throw new MetierException("Veuillez renseigner un email");
        }
    }

    // Refuse un email déjà utilisé (casse ignorée) par un autre contact de la
    // même entreprise ; un email absent ou vide est autorisé (pas de doublon).
    private void validerUniciteEmail(ContactForm contactForm, int entrepriseId) {
        String email = contactForm.getEmail();
        if (email != null && !email.isBlank()
                && contactRepository.existsByTitulaireAndEntrepriseIdAndEmailIgnoreCase(
                        utilisateurCourant.titulaire(), entrepriseId, email)) {
            log.warn("création de contact refusée : email en doublon pour l'entreprise {}", entrepriseId);
            throw new MetierException("Un contact avec cet email existe déjà pour cette entreprise.");
        }
    }

    /**
     * Retourne tous les contacts d'une entreprise.
     *
     * <p><b>Exemple :</b> getAllContact(3) retourne les contacts rattachés à l'entreprise 3, ou une liste vide si elle n'en a aucun.</p>
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return la liste des contacts de l'entreprise
     */
    public List<Contact> getAllContact(int entrepriseId) {
        return contactRepository.findAllByEntrepriseIdAndTitulaire(entrepriseId, utilisateurCourant.titulaire());
    }


    /**
     * Récupère un contact par son identifiant.
     *
     * <p><b>Exemple :</b> getContact(3L) retourne le contact d'id 3 ; un id inexistant lève NoSuchElementException.</p>
     *
     * @param contactId identifiant du contact
     * @return le contact correspondant
     * @throws java.util.NoSuchElementException si aucun contact ne correspond à l'identifiant
     */
    public Contact getContact(Long contactId) {
        return contactRepository.findByIdAndTitulaire(contactId, utilisateurCourant.titulaire()).orElseThrow();
    }

    /**
     * Charge en une seule requête tous les contacts demandés, indexés par id.
     * Évite le problème N+1 lors de la construction des listes d'annonces.
     *
     * <p><b>Exemple :</b> getContactsByIds([2L, 5L]) retourne une map {2 -> contact 2, 5 -> contact 5} en une seule requête ; une collection nulle ou vide retourne une map vide.</p>
     *
     * @param ids identifiants des contacts à charger (peut être nul ou vide)
     * @return une map des contacts trouvés indexés par identifiant, vide si aucun id
     */
    public Map<Long, Contact> getContactsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return contactRepository.findAllByIdInAndTitulaire(ids, utilisateurCourant.titulaire()).stream()
                .collect(Collectors.toMap(Contact::getId, Function.identity()));
    }
}
