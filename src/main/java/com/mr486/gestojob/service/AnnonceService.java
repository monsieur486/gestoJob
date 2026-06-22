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

/**
 * Service métier de gestion des annonces (candidatures).
 * Gère la création, la consultation, le changement de statut, l'envoi par email
 * et la construction des contenus (HTML et texte) des annonces.
 */
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

    // Récupère toutes les annonces d'une entreprise, triées par statut croissant.
    private List<Annonce> findAllByEntrepriseId(Integer entrepriseId) {
        return annonceRepository.findAllByEntrepriseIdOrderByStatusAnnonceAsc(entrepriseId);
    }

    /**
     * Récupère une annonce par son identifiant.
     *
     * <p><b>Exemple :</b> getAnnonce(7L) retourne l'annonce d'id 7 ; un id inexistant lève NoSuchElementException.</p>
     *
     * @param annonceId identifiant de l'annonce
     * @return l'annonce correspondante
     * @throws java.util.NoSuchElementException si aucune annonce ne correspond à l'identifiant
     */
    public Annonce getAnnonce(Long annonceId) {
        return annonceRepository.findById(annonceId).orElseThrow();
    }

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
        Long contactId = getAnnonce(annonceId).getContactId();
        if (contactId == null) {
            return "Madame, Monsieur,";
        }
        return contactService.getContact(contactId).getMessageDePolitesse();
    }

    /**
     * Retourne la liste des annonces d'une entreprise au format DTO,
     * triées par date d'envoi décroissante (les annonces sans date en dernier).
     *
     * <p><b>Exemple :</b> pour deux annonces datées des 10/06 et 12/06, celle du 12/06 apparaît en première position ; une annonce sans date d'envoi est placée en dernier.</p>
     *
     * @param entrepriseId identifiant de l'entreprise
     * @return la liste des annonces de l'entreprise
     */
    public List<AnnonceListe> annoncesListeByEntrepriseId(Integer entrepriseId) {
        List<Annonce> annoncesEntreprise = findAllByEntrepriseId(entrepriseId);
        // Tri sur la vraie date (OffsetDateTime), du plus récent au plus ancien,
        // les annonces sans date d'envoi étant placées en dernier.
        annoncesEntreprise.sort(
                Comparator.comparing(Annonce::getDateEnvoi,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return toAnnonceListe(annoncesEntreprise);
    }

    // Persiste une annonce en base.
    private void save(Annonce a) {
        annonceRepository.save(a);
    }

    /**
     * Crée et enregistre une annonce à partir du formulaire de saisie.
     * Une candidature sans contact (réponse via le site) est marquée comme déjà
     * envoyée (statut 2) avec la date du jour ; sinon elle est mise en attente (statut 1).
     *
     * <p><b>Exemple :</b> un formulaire avec contactId=null est enregistré au statut 2 (envoyé) avec la date du jour ; avec un contactId=3, l'annonce est créée au statut 1 (en attente) et sans date d'envoi.</p>
     *
     * @param form formulaire de l'annonce
     * @throws RuntimeException si aucune entreprise n'est renseignée
     */
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

    /**
     * Retourne la liste des annonces en attente d'envoi par email (statut 1).
     *
     * <p><b>Exemple :</b> ne retourne que les annonces au statut 1 ; les annonces aux statuts 2 à 6 sont exclues.</p>
     *
     * @return la liste des annonces en attente
     */
    public List<AnnonceListe> annoncesEnAttenteEnvoiEmail() {
        return toAnnonceListe(annonceRepository.findAllByStatusAnnonce(1));
    }

    // Met à jour le statut d'une annonce en base.
    private void updateStatusAnnonce(Long annonceId, Integer statusAnnonce) {
        annonceRepository.updateStatusAnnonce(annonceId, statusAnnonce);
    }

    /**
     * Marque l'annonce comme envoyée (statut 2) et fixe sa date d'envoi à maintenant.
     *
     * <p><b>Exemple :</b> setEnvoye(7L) fait passer l'annonce 7 au statut 2 et fixe sa date d'envoi à l'instant courant.</p>
     *
     * @param annonceId identifiant de l'annonce
     */
    @Transactional
    public void setEnvoye(Long annonceId) {
        annonceRepository.updateStatusAnnonceEtDateEnvoi(annonceId, 2, java.time.OffsetDateTime.now());
    }

    /**
     * Marque l'annonce comme dépassée / sans suite (statut 3).
     *
     * <p><b>Exemple :</b> setDepace(7L) fait passer l'annonce 7 au statut 3 (dépassée).</p>
     *
     * @param annonceId identifiant de l'annonce
     */
    @Transactional
    public void setDepace(Long annonceId) {
        updateStatusAnnonce(annonceId, 3);
    }

    /**
     * Marque l'annonce comme refusée (statut 4).
     *
     * <p><b>Exemple :</b> setRefus(7L) fait passer l'annonce 7 au statut 4 (refusée).</p>
     *
     * @param annonceId identifiant de l'annonce
     */
    @Transactional
    public void setRefus(Long annonceId) {
        updateStatusAnnonce(annonceId, 4);
    }

    /**
     * Marque l'annonce comme acceptée (statut 5).
     *
     * <p><b>Exemple :</b> setAccepte(7L) fait passer l'annonce 7 au statut 5 (acceptée / positif).</p>
     *
     * @param annonceId identifiant de l'annonce
     */
    @Transactional
    public void setAccepte(Long annonceId) {
        updateStatusAnnonce(annonceId, 5);
    }

    /**
     * Tente d'envoyer chaque annonce en attente. Un échec sur une annonce est
     * journalisé mais n'interrompt pas la boucle ; seules les annonces réellement
     * envoyées passent au statut "envoyé".
     *
     * <p><b>Exemple :</b> sur 3 annonces au statut 1, si l'envoi de la deuxième échoue, les annonces 1 et 3 passent au statut 2 tandis que la 2 reste au statut 1.</p>
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
        String messageDePolitesse = (annonce.getContactId() == null)
                ? "Madame, Monsieur,"
                : contactService.getContact(annonce.getContactId()).getMessageDePolitesse();

        return contenuService.getHtmlContenu(
                annonce.getPoste(),
                annonce.getTypeContenu(),
                messageDePolitesse
        );
    }

    // Construit la cha\u00EEne d'information affich\u00E9e dans la liste (entreprise, canal
    // de contact email ou site, et type de contenu : MS pour microservices, G sinon).
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

    // Convertit une annonce en DTO d'affichage en résolvant l'entreprise et le
    // contact depuis les maps pré-chargées, et en formatant date, type et statut.
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

    /**
     * Recherche paginée d'annonces. Sans texte de recherche : retourne soit toutes
     * les annonces (avec archives), soit uniquement les envoyées (statut 2). Avec un
     * texte : effectue une recherche multi-champs, élargie à tous les statuts si les
     * archives sont incluses.
     *
     * <p><b>Exemple :</b> sans texte et avecArchives=false, ne retourne que les annonces au statut 2 ; avec le texte « Java » et avecArchives=true, recherche « Java » sur tous les statuts.</p>
     *
     * @param form      critères de recherche (texte et inclusion des archives), peut être null
     * @param pageIndex index de la page (commençant à 0)
     * @return la page de résultats au format DTO
     */
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

    /**
     * Retourne le nombre total d'annonces enregistrées.
     *
     * <p><b>Exemple :</b> avec 12 annonces en base, countAnnonces() retourne 12.</p>
     *
     * @return le nombre d'annonces
     */
    public long countAnnonces() {
        return annonceRepository.count();
    }

    /**
     * Retourne une page d'annonces ayant abouti positivement (statut 5, acceptées),
     * triées par date d'envoi décroissante.
     *
     * <p><b>Exemple :</b> getAllPositifListePage(0) retourne uniquement des annonces au statut 5, de la plus récente à la plus ancienne.</p>
     *
     * @param pageIndex index de la page (commençant à 0)
     * @return la page d'annonces positives au format DTO
     */
    public Page<AnnonceListe> getAllPositifListePage(int pageIndex) {
        var pageable = PageRequest.of(pageIndex, maxPositifsParPage);
        return toAnnonceListePage(annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(5, pageable));
    }

    /**
     * Construit la version texte du contenu d'une annonce (libellé suivi du corps),
     * destinée par exemple au copier-coller.
     *
     * <p><b>Exemple :</b> getAnnonceTxtContenuById(7L) retourne le libellé de l'annonce suivi de deux sauts de ligne puis du corps texte (non échappé) intégrant la formule de politesse.</p>
     *
     * @param id identifiant de l'annonce
     * @return le contenu texte de l'annonce
     * @throws IllegalArgumentException si l'annonce est introuvable
     */
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
