package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.persistance.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
public class ContactService {

    private final ContactRepository contactRepository;
    private final EntrepriseService entrepriseService;

    /**
     * Crée et enregistre un contact pour une entreprise.
     *
     * <p><b>Exemple :</b> un formulaire avec formuleDePolitesse=1 et email vide lève RuntimeException(« Veuillez renseigner un email »).</p>
     *
     * @param contactForm  formulaire du contact
     * @param entrepriseId identifiant de l'entreprise rattachée
     * @throws RuntimeException si l'entreprise est introuvable, ou si une formule de
     *                          politesse est choisie sans email renseigné
     */
    public void saveContact(ContactForm contactForm, int entrepriseId) {
        if (!entrepriseService.existe(entrepriseId)) {
            throw new RuntimeException("Entreprise introuvable avec id: " + entrepriseId);
        }
        if (contactForm.getFormuleDePolitesse() > 0 && contactForm.getEmail().isBlank()) {
            throw new RuntimeException("Veuillez renseigner un email");
        }

        Contact contact = new Contact();
        contact.setEntrepriseId(entrepriseId);
        contact.setFormuleDePolistesse(contactForm.getFormuleDePolitesse());
        contact.setEmail(contactForm.getEmail());
        contact.setContact(contactForm.getContact());
        contactRepository.save(contact);
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
