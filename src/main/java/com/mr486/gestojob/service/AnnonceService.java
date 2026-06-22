package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.dto.RechercheAnnonceForm;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.tools.MailTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnonceService {

    private static final DateTimeFormatter FR_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final AnnonceRepository annonceRepository;
    private final EntrepriseService entrepriseService;
    private final ContactService contactService;
    private final MailTools mailTools;
    private final ContenuService contenuService;
    @Value("${gestojob.max-annonces-par-page:7}")
    private int maxAnnoncesParPage;

    @Value("${gestojob.max-positifs-par-page:7}")
    private int maxPositifsParPage;

    private List<Annonce> findAllByEntrepriseId(Integer entrepriseId) {
        return annonceRepository.findAllByEntrepriseIdOrderByStatusAnnonceAsc(entrepriseId);
    }

    public Annonce getAnnonce(Long annonceId) {
        return annonceRepository.findById(annonceId).orElseThrow();
    }

    public String getMessageDePolitesse(Long annonceId) {
        Long contactId = getAnnonce(annonceId).getContactId();
        if (contactId == null) {
            return "Madame, Monsieur,";
        }
        return contactService.getContact(contactId).getMessageDePolitesse();
    }

    public List<AnnonceListe> annoncesListeByEntrepriseId(Integer entrepriseId) {
        List<Annonce> annoncesEntreprise = findAllByEntrepriseId(entrepriseId);
        // Tri sur la vraie date (OffsetDateTime), du plus récent au plus ancien,
        // les annonces sans date d'envoi étant placées en dernier.
        annoncesEntreprise.sort(
                Comparator.comparing(Annonce::getDateEnvoi,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return toAnnonceListe(annoncesEntreprise);
    }

    private void save(Annonce a) {
        annonceRepository.save(a);
    }

    public void saveForm(AnnonceForm form) {
        if (form.getEntrepriseId() == null) {
            throw new RuntimeException("Entreprise obligatoire pour créer une annonce.");
        }

        Long contactId = form.getContactId();
        boolean site = (contactId == null || contactId == 0L);

        Annonce a = Annonce.builder()
                .entrepriseId(form.getEntrepriseId())
                .contactId(site ? null : contactId)
                .typeAnnonce(form.getTypeAnnonce())
                .typeContenu(form.getContenuId())
                .poste(form.getPoste())
                .reference(form.getReference())
                .statusAnnonce(site ? 2 : 1)
                .dateEnvoi(site ? OffsetDateTime.now() : null)
                .build();

        save(a);
    }

    public List<AnnonceListe> annoncesEnAttenteEnvoiEmail() {
        return toAnnonceListe(annonceRepository.findAllByStatusAnnonce(1));
    }

    private void updateStatusAnnonce(Long annonceId, Integer statusAnnonce) {
        annonceRepository.updateStatusAnnonce(annonceId, statusAnnonce);
    }

    @Transactional
    public void setEnvoye(Long annonceId) {
        annonceRepository.updateStatusAnnonceEtDateEnvoi(annonceId, 2, java.time.OffsetDateTime.now());
    }

    @Transactional
    public void setDepace(Long annonceId) {
        updateStatusAnnonce(annonceId, 3);
    }

    @Transactional
    public void setRefus(Long annonceId) {
        updateStatusAnnonce(annonceId, 4);
    }

    @Transactional
    public void setAccepte(Long annonceId) {
        updateStatusAnnonce(annonceId, 5);
    }

    /**
     * Tente d'envoyer chaque annonce en attente. Un échec sur une annonce est
     * journalisé mais n'interrompt pas la boucle ; seules les annonces réellement
     * envoyées passent au statut "envoyé".
     */
    @Transactional
    public void sendEmailForPendingAnnonces() {
        List<Annonce> annoncesEnAttente = annonceRepository.findAllByStatusAnnonce(1);
        for (Annonce a : annoncesEnAttente) {
            try {
                log.info("Envoi de l'email pour l'annonce id: {}", a.getId());
                sendMail(a);
                setEnvoye(a.getId());
            } catch (RuntimeException ex) {
                log.error("Échec de l'envoi de l'email pour l'annonce id={}, statut inchangé : {}",
                        a.getId(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void sendDirectEmail(Long annonceId) {
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new IllegalArgumentException("Annonce introuvable avec id: " + annonceId));
        log.info("Envoi de l'email pour l'annonce id: {}", annonce.getId());
        // Si l'envoi échoue, l'exception remonte et le statut n'est PAS modifié.
        sendMail(annonce);
        setEnvoye(annonce.getId());
    }

    /**
     * Construit et envoie l'email. Lève une exception si l'envoi échoue,
     * pour que l'appelant ne marque pas l'annonce comme envoyée à tort.
     */
    private void sendMail(Annonce annonce) {
        Contact contact = contactService.getContact(annonce.getContactId());
        String recipient = contact.getEmail();
        String subject = annonce.getLibelle();
        String content = getHtmlContent(annonce);
        mailTools.sendHtmlMail(recipient, subject, content);
    }

    public String getHtmlContent(Annonce annonce) {
        String messageDePolitesse = (annonce.getContactId() == null)
                ? "Madame, Monsieur,"
                : contactService.getContact(annonce.getContactId()).getMessageDePolitesse();

        return contenuService.getHtmlContenu(
                annonce.getPoste(),
                annonce.getTypeContenu(),
                messageDePolitesse
        );
    }

    private String getInfos(Annonce annonce, Entreprise entreprise, Contact contact) {
        String result = "\uD83C\uDFE2";
        result += (entreprise != null ? entreprise.getNom() : "?");
        if (annonce.getContactId() != null && contact != null) {
            result += " \uD83D\uDD82" + contact.getEmail();
        } else {
            result += " \uD83C\uDF10site";
        }
        if (annonce.getTypeContenu() != null && annonce.getTypeContenu() == 1) {
            result += " MS";
        } else {
            result += " G";
        }
        return result;
    }

    /**
     * Convertit une page d'annonces en page de DTO en chargeant les entreprises
     * et contacts li\u00E9s en lot (\u00E9vite le N+1).
     */
    private Page<AnnonceListe> toAnnonceListePage(Page<Annonce> page) {
        List<AnnonceListe> content = toAnnonceListe(page.getContent());
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    /**
     * Convertit une liste d'annonces en DTO en chargeant en deux requ\u00EAtes
     * (une pour les entreprises, une pour les contacts) toutes les r\u00E9f\u00E9rences.
     */
    private List<AnnonceListe> toAnnonceListe(List<Annonce> annonces) {
        Set<Integer> entrepriseIds = annonces.stream()
                .map(Annonce::getEntrepriseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contactIds = annonces.stream()
                .map(Annonce::getContactId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, Entreprise> entreprises = entrepriseService.getEntreprisesByIds(entrepriseIds);
        Map<Long, Contact> contacts = contactService.getContactsByIds(contactIds);

        return annonces.stream()
                .map(a -> fromEntity(a, entreprises, contacts))
                .collect(Collectors.toList());
    }

    private AnnonceListe fromEntity(Annonce annonce, Map<Integer, Entreprise> entreprises,
                                    Map<Long, Contact> contacts) {
        Entreprise entreprise = entreprises.get(annonce.getEntrepriseId());
        Contact contact = annonce.getContactId() != null ? contacts.get(annonce.getContactId()) : null;

        AnnonceListe liste = new AnnonceListe();
        liste.setId(annonce.getId());
        String dateEnvoi = annonce.getDateEnvoi() != null
                ? annonce.getDateEnvoi().format(FR_DATE_TIME)
                : "--";
        liste.setDateEnvoi(dateEnvoi);
        if (annonce.getTypeAnnonce() != null && annonce.getTypeAnnonce() == 0) {
            liste.setType("\uD83C\uDD93 S");
        } else {
            liste.setType("\uD83D\uDCDD A");
        }
        liste.setLibelle(annonce.getLibelle());
        liste.setStatus(annonce.getStatusAnnonceString());
        liste.setInfo(getInfos(annonce, entreprise, contact));
        liste.setEntrepriseId(annonce.getEntrepriseId());
        return liste;
    }

    public Page<AnnonceListe> searchAnnoncesPage(RechercheAnnonceForm form, int pageIndex) {
        String q = (form != null && form.getRecherche() != null) ? form.getRecherche().trim() : "";
        boolean includeArchives = form != null && Boolean.TRUE.equals(form.getAvecArchives());

        var pageable = PageRequest.of(pageIndex, maxAnnoncesParPage);

        if (q.isBlank()) {
            // ✅ Pas de texte : si archives=true => toutes les annonces, sinon seulement "en attente"
            return toAnnonceListePage(includeArchives
                    ? annonceRepository.findAllOrderByDateEnvoiDesc(pageable)
                    : annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(2, pageable));
        }

        // ✅ Texte présent : recherche multi-champs, et archives=true => tous status
        return toAnnonceListePage(annonceRepository.search(q, includeArchives, pageable));
    }

    public long countAnnonces() {
        return annonceRepository.count();
    }

    public Page<AnnonceListe> getAllPositifListePage(int pageIndex) {
        var pageable = PageRequest.of(pageIndex, maxPositifsParPage);
        return toAnnonceListePage(annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(5, pageable));
    }

    public String getAnnonceTxtContenuById(Long id) {
        Annonce annonce = getAnnonce(id);
        if (annonce == null) {
            throw new IllegalArgumentException("Contenu non trouvé");
        }
        String result = annonce.getLibelle() + "\n\n";
        String messageDePolitesse = getMessageDePolitesse(id);

        result += contenuService.getTextContenu(annonce.getPoste(), annonce.getTypeContenu(), messageDePolitesse);

        return result;
    }
}
