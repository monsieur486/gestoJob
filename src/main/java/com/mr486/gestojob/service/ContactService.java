package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.persistance.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * Supprime un contact, sauf s'il est encore rattaché à des annonces.
     *
     * <p><b>Exemple :</b> deleteContact(9L) supprime le contact 9 s'il n'est lié à aucune annonce ; sinon lève RuntimeException(« … rattaché à des annonces … »).</p>
     *
     * @param contactId identifiant du contact à supprimer
     * @throws RuntimeException si le contact est encore référencé par une annonce
     */
    @Transactional
    public void deleteContact(Long contactId) {
        if (annonceRepository.existsByContactId(contactId)) {
            log.warn("suppression refusée : le contact {} est rattaché à des annonces", contactId);
            throw new RuntimeException("Ce contact est rattaché à des annonces ; supprimez-les d'abord.");
        }
        contactRepository.deleteById(contactId);
        log.info("contact supprimé : {}", contactId);
    }

    /**
     * Crée et enregistre un contact pour une entreprise.
     *
     * <p><b>Exemple :</b> un formulaire avec formuleDePolitesse=1 et email vide lève RuntimeException(« Veuillez renseigner un email »).</p>
     *
     * @param contactForm  formulaire du contact
     * @param entrepriseId identifiant de l'entreprise rattachée
     * @throws RuntimeException si l'entreprise est introuvable, si une formule de
     *                          politesse est choisie sans email renseigné, ou si un
     *                          contact avec le même email existe déjà pour l'entreprise
     */
    public void saveContact(ContactForm contactForm, int entrepriseId) {
        if (!entrepriseService.existe(entrepriseId)) {
            log.warn("création de contact refusée : entreprise {} introuvable", entrepriseId);
            throw new RuntimeException("Entreprise introuvable avec id: " + entrepriseId);
        }
        Integer formuleDePolitesse = contactForm.getFormuleDePolitesse();
        String email = contactForm.getEmail();
        if (formuleDePolitesse != null && formuleDePolitesse > 0 && (email == null || email.isBlank())) {
            log.warn("création de contact refusée : formule de politesse sans email (entreprise {})", entrepriseId);
            throw new RuntimeException("Veuillez renseigner un email");
        }
        if (email != null && !email.isBlank()
                && contactRepository.existsByEntrepriseIdAndEmailIgnoreCase(entrepriseId, email)) {
            log.warn("création de contact refusée : email en doublon pour l'entreprise {}", entrepriseId);
            throw new RuntimeException("Un contact avec cet email existe déjà pour cette entreprise.");
        }

        Contact contact = new Contact();
        contact.setEntrepriseId(entrepriseId);
        contact.setFormuleDePolitesse(contactForm.getFormuleDePolitesse());
        contact.setEmail(contactForm.getEmail());
        contact.setNom(contactForm.getNom());
        contactRepository.save(contact);
        log.info("contact créé pour l'entreprise {}", entrepriseId);
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
        return contactRepository.findAllByEntrepriseId(entrepriseId);
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
        return contactRepository.findById(contactId).orElseThrow();
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
        return contactRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Contact::getId, Function.identity()));
    }
}
