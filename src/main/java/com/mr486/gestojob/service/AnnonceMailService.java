package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.StatutAnnonce;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.tools.MailTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service d'envoi des emails de candidature. Responsable de la construction du
 * corps HTML, de l'expédition via {@link MailTools} et du passage de l'annonce
 * au statut « envoyé » une fois l'email parti.
 * <p>
 * Extrait d'{@link AnnonceService} pour isoler la responsabilité d'envoi
 * (principe de responsabilité unique).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnonceMailService {

    private final AnnonceRepository annonceRepository;
    private final ContactService contactService;
    private final ContenuService contenuService;
    private final MailTools mailTools;
    private final LibelleService libelleService;

    /**
     * Tente d'envoyer chaque annonce en attente. Un échec sur une annonce est
     * journalisé mais n'interrompt pas la boucle ; seules les annonces réellement
     * envoyées passent au statut « envoyé ».
     *
     * <p><b>Exemple :</b> sur 3 annonces au statut 1, si l'envoi de la deuxième échoue, les annonces 1 et 3 passent au statut 2 tandis que la 2 reste au statut 1.</p>
     */
    @Transactional
    public void sendEmailForPendingAnnonces() {
        List<Annonce> annoncesEnAttente =
                annonceRepository.findAllByStatusAnnonce(StatutAnnonce.BOITE_ENVOI.getCode());
        // Chargement groupé des contacts (une seule requête) pour éviter le N+1
        // d'un getContact par annonce dans la boucle d'envoi.
        Set<Long> contactIds = annoncesEnAttente.stream()
                .map(Annonce::getContactId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Contact> contacts = contactService.getContactsByIds(contactIds);
        int envoyees = 0;
        for (Annonce a : annoncesEnAttente) {
            try {
                sendMail(a, contacts.get(a.getContactId()));
                marquerEnvoye(a.getId());
                envoyees++;
                log.info("email envoyé pour l'annonce id={}", a.getId());
            } catch (RuntimeException ex) {
                log.error("échec de l'envoi de l'email pour l'annonce id={}, statut inchangé : {}",
                        a.getId(), ex.getMessage(), ex);
            }
        }
        log.info("envoi de la file d'attente terminé : {} annonce(s) envoyée(s) sur {}",
                envoyees, annoncesEnAttente.size());
    }

    /**
     * Envoie immédiatement l'email d'une annonce puis la marque comme envoyée.
     * Si l'envoi échoue, l'exception remonte et le statut n'est pas modifié.
     *
     * <p><b>Exemple :</b> sendDirectEmail(7L) envoie l'email puis passe l'annonce 7 au statut 2 ; si l'envoi échoue, l'annonce reste au statut 1.</p>
     *
     * @param annonceId identifiant de l'annonce
     * @throws IllegalArgumentException si l'annonce est introuvable
     */
    @Transactional
    public void sendDirectEmail(Long annonceId) {
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new IllegalArgumentException("Annonce introuvable avec id: " + annonceId));
        log.info("envoi direct de l'email pour l'annonce id={}", annonce.getId());
        // Si l'envoi échoue, l'exception remonte et le statut n'est PAS modifié.
        Contact contact = (annonce.getContactId() == null)
                ? null
                : contactService.getContact(annonce.getContactId());
        sendMail(annonce, contact);
        marquerEnvoye(annonce.getId());
    }

    /**
     * Construit le corps HTML de l'email pour une annonce, en y insérant la
     * formule de politesse adaptée au contact (ou générique si absent).
     *
     * <p><b>Exemple :</b> pour une annonce sans contact, le HTML produit contient la formule « Madame, Monsieur, » ; avec un contact, il reprend sa formule de politesse personnalisée.</p>
     *
     * @param annonce l'annonce concernée
     * @return le contenu HTML de l'email
     */
    public String getHtmlContent(Annonce annonce) {
        Contact contact = (annonce.getContactId() == null)
                ? null
                : contactService.getContact(annonce.getContactId());
        return buildHtmlContent(annonce, contact);
    }

    // Marque l'annonce comme envoyée (statut 2) et fixe sa date d'envoi à maintenant.
    private void marquerEnvoye(Long annonceId) {
        annonceRepository.updateStatusAnnonceEtDateEnvoi(
                annonceId, StatutAnnonce.EN_COURS.getCode(), OffsetDateTime.now());
    }

    // Construit et envoie l'email à partir du contact déjà résolu. Lève une
    // exception si l'envoi échoue, pour que l'appelant ne marque pas l'annonce
    // comme envoyée à tort.
    private void sendMail(Annonce annonce, Contact contact) {
        if (contact == null) {
            // Une annonce sans contact (candidature « site ») ne doit jamais passer
            // par l'envoi d'email : on échoue explicitement plutôt que de risquer une NPE.
            throw new IllegalStateException(
                    "Envoi impossible : l'annonce id=" + annonce.getId() + " n'a pas de contact.");
        }
        String recipient = contact.getEmail();
        String subject = libelleService.construitLibelle(annonce);
        String content = buildHtmlContent(annonce, contact);
        mailTools.sendHtmlMail(recipient, subject, content);
    }

    // Construit le corps HTML à partir d'un contact déjà résolu (ou null pour
    // une formule générique), sans relire la base.
    private String buildHtmlContent(Annonce annonce, Contact contact) {
        String messageDePolitesse = (contact == null)
                ? ApplicationConfiguration.SALUTATION_GENERIQUE
                : contact.getMessageDePolitesse();

        return contenuService.getHtmlContenu(
                annonce.getPoste(),
                annonce.getTypeContenu(),
                messageDePolitesse
        );
    }
}
