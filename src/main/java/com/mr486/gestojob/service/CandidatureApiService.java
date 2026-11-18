package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.dto.api.CandidatureDemande;
import com.mr486.gestojob.dto.api.CandidatureReponse;
import com.mr486.gestojob.dto.api.ContactApi;
import com.mr486.gestojob.dto.api.ContactDemande;
import com.mr486.gestojob.dto.api.EntrepriseApi;
import com.mr486.gestojob.dto.api.EntrepriseDemande;
import com.mr486.gestojob.dto.api.ModeleLettreApi;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.model.TypeContenu;
import com.mr486.gestojob.persistance.ContactRepository;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de l'API de gestion d'annonces : expose le modèle de lettre, la
 * recherche d'entreprises (avec leurs contacts) et la création d'une candidature
 * complète (entreprise, contact, annonce avec lettre personnalisée). Toutes les
 * lectures et écritures sont limitées au titulaire courant.
 */
// Orchestrateur de la création d'une candidature : le couplage élevé est inhérent à son rôle.
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CouplingBetweenObjects"})
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidatureApiService {

    private final EntrepriseService entrepriseService;
    private final ContactService contactService;
    private final AnnonceService annonceService;
    private final ModeleEmailService modeleEmailService;
    private final ProfilService profilService;
    private final EntrepriseRepository entrepriseRepository;
    private final ContactRepository contactRepository;
    private final UtilisateurCourant utilisateurCourant;
    private final Validator validator;

    /**
     * Renvoie le modèle de lettre « IA agentique » du titulaire courant et son poste par défaut.
     *
     * <p><b>Exemple :</b> pour LAURENT, renvoie la clé {@code CONTENU_IA}, le HTML du modèle et le poste par défaut de son profil.</p>
     *
     * @return le modèle de lettre
     */
    public ModeleLettreApi modeleLettre() {
        Titulaire titulaire = utilisateurCourant.titulaire();
        String html = modeleEmailService.getContenu(titulaire, CleModele.CONTENU_IA.name());
        String poste = profilService.posteParDefaut(profilService.getProfil(titulaire), CleModele.CONTENU_IA);
        return new ModeleLettreApi(CleModele.CONTENU_IA.name(), html, poste);
    }

    /**
     * Recherche les entreprises du titulaire dont le nom contient le texte donné,
     * avec leurs contacts (chargés en une seule requête).
     *
     * <p><b>Exemple :</b> {@code rechercherEntreprises("acm")} renvoie au plus 20 entreprises dont le nom contient « acm » ; un nom blanc lève une {@link MetierException}.</p>
     *
     * @param nom texte recherché (obligatoire)
     * @return les entreprises trouvées, triées par nom
     * @throws MetierException si le nom est nul ou blanc
     */
    @Transactional(readOnly = true)
    public List<EntrepriseApi> rechercherEntreprises(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new MetierException("Le nom recherché est obligatoire.");
        }
        Titulaire titulaire = utilisateurCourant.titulaire();
        List<Entreprise> entreprises = entrepriseRepository
                .findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc(titulaire, nom.trim(),
                        PageRequest.of(0, ApplicationConfiguration.MAX_RESULTATS_RECHERCHE_API))
                .getContent();
        if (entreprises.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = entreprises.stream().map(Entreprise::getId).toList();
        Map<Integer, List<ContactApi>> contactsParEntreprise = contactRepository
                .findAllByEntrepriseIdInAndTitulaire(ids, titulaire).stream()
                .collect(Collectors.groupingBy(Contact::getEntrepriseId,
                        Collectors.mapping(CandidatureApiService::versContactApi, Collectors.toList())));
        return entreprises.stream()
                .map(e -> new EntrepriseApi(e.getId(), e.getNom(), e.getVille(), e.getCodePostal(),
                        e.getEstActive(), contactsParEntreprise.getOrDefault(e.getId(), List.of())))
                .toList();
    }

    /**
     * Crée une candidature : résout ou crée l'entreprise (réactivée si inactive) et
     * le contact, puis crée l'annonce avec sa lettre personnalisée.
     *
     * <p><b>Exemple :</b> avec une entreprise existante et sans contact, crée une annonce « site » déjà envoyée ; avec un contact rattaché à une autre entreprise, lève {@link NoSuchElementException}.</p>
     *
     * @param demande la demande de candidature
     * @return les identifiants et indicateurs de ce qui a été créé
     * @throws NoSuchElementException si l'entreprise ou le contact est inconnu (ou d'un autre titulaire)
     * @throws MetierException        si les données d'une entreprise ou d'un contact à créer sont invalides
     */
    @Transactional
    public CandidatureReponse creer(CandidatureDemande demande) {
        EntrepriseDemande ed = demande.entreprise();
        ContactDemande cd = demande.contact();
        boolean entrepriseCreee = ed.id() == null;

        // Phase 1 : toutes les vérifications en lecture seule, avant la moindre écriture.
        Entreprise existante = entrepriseCreee ? null : entrepriseService.getEntreprise(ed.id());
        EntrepriseForm formEntreprise = entrepriseCreee ? formEntreprise(ed) : null;
        ContactForm formContact = cd != null && cd.id() == null ? formContact(cd) : null;
        Contact contactExistant = cd != null && cd.id() != null ? contactRattache(cd.id(), existante) : null;

        // Phase 2 : écritures.
        int entrepriseId = entrepriseCreee ? entrepriseService.save(formEntreprise) : existante.getId();
        boolean entrepriseReactivee = !entrepriseCreee && reactiver(existante);
        Long contactId = enregistrerContact(formContact, contactExistant, entrepriseId);
        boolean contactCree = formContact != null;

        AnnonceForm form = new AnnonceForm(entrepriseId, TypeContenu.IA.getCode(), contactId,
                demande.annonce().typeAnnonce(), demande.annonce().poste(), demande.annonce().reference(), null);
        Annonce annonce = annonceService.creerAvecLettre(form, demande.lettreHtml());
        log.info("candidature API créée : annonce {} (entreprise {}, créée={}, réactivée={})",
                annonce.getId(), entrepriseId, entrepriseCreee, entrepriseReactivee);
        return new CandidatureReponse(annonce.getId(), entrepriseId, contactId, annonce.getStatusAnnonce(),
                entrepriseCreee, entrepriseReactivee, contactCree);
    }

    // Renvoie le contact du titulaire, qui doit être rattaché à l'entreprise existante donnée (sinon 404).
    private Contact contactRattache(Long contactId, Entreprise existante) {
        Contact contact = contactService.getContact(contactId);
        // Un contact d'une autre entreprise (ou avec une entreprise à créer) est traité comme inconnu (404).
        if (existante == null || !existante.getId().equals(contact.getEntrepriseId())) {
            throw new NoSuchElementException("Contact introuvable avec id: " + contactId);
        }
        return contact;
    }

    // Crée le contact si un formulaire est fourni, sinon reprend l'existant ; null s'il n'y a pas de contact.
    private Long enregistrerContact(ContactForm formContact, Contact contactExistant, int entrepriseId) {
        if (formContact != null) {
            return contactService.saveContact(formContact, entrepriseId);
        }
        return contactExistant == null ? null : contactExistant.getId();
    }

    // Réactive l'entreprise si elle est inactive ; indique si une réactivation a eu lieu.
    private boolean reactiver(Entreprise existante) {
        if (Boolean.TRUE.equals(existante.getEstActive())) {
            return false;
        }
        entrepriseService.setActive(existante.getId(), true);
        return true;
    }

    // Construit explicitement le formulaire (sans les valeurs par défaut de ville et code postal) et le valide.
    private EntrepriseForm formEntreprise(EntrepriseDemande ed) {
        EntrepriseForm form = new EntrepriseForm(ed.nom(), true, ed.adresse1(), ed.adresse2(),
                ed.codePostal(), ed.ville());
        valider(form);
        return form;
    }

    // Construit le formulaire de contact depuis la demande et le valide.
    private ContactForm formContact(ContactDemande cd) {
        Integer politesse = cd.formuleDePolitesse();
        ContactForm form = new ContactForm(cd.email(), politesse == null ? Integer.valueOf(0) : politesse, cd.nom());
        valider(form);
        return form;
    }

    // Lève une MetierException dont le message joint toutes les violations par « ; ».
    private void valider(Object form) {
        String messages = validator.validate(form).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining(" ; "));
        if (!messages.isEmpty()) {
            throw new MetierException(messages);
        }
    }

    // Convertit un contact en vue d'API.
    private static ContactApi versContactApi(Contact c) {
        return new ContactApi(c.getId(), c.getNom(), c.getEmail(), c.getFormuleDePolitesse());
    }
}
