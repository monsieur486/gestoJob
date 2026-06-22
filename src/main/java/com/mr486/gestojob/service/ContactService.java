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

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final EntrepriseService entrepriseService;

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

    public List<Contact> getAllContact(int entrepriseId) {
        return contactRepository.findAllByEntrepriseId(entrepriseId);
    }


    public Contact getContact(Long contactId) {
        return contactRepository.findById(contactId).orElseThrow();
    }

    /**
     * Charge en une seule requête tous les contacts demandés, indexés par id.
     * Évite le problème N+1 lors de la construction des listes d'annonces.
     */
    public Map<Long, Contact> getContactsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return contactRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Contact::getId, Function.identity()));
    }
}
