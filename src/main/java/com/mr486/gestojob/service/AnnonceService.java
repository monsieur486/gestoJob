package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.dto.RechercheAnnonceForm;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.persistance.AnnonceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Service métier de gestion des annonces (candidatures).
 * Gère la création, la consultation, le changement de statut, la recherche
 * paginée et la construction du contenu texte des annonces. L'envoi des emails
 * est délégué à {@link AnnonceMailService}.
 */
@Service
@RequiredArgsConstructor
public class AnnonceService {

    private final AnnonceRepository annonceRepository;
    private final ContactService contactService;
    private final ContenuService contenuService;
    private final AnnonceListeMapper annonceListeMapper;
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
        return annonceListeMapper.toAnnonceListe(annoncesEntreprise);
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
     * Retourne une page d'annonces en attente d'envoi par email (statut 1),
     * triées par date d'envoi décroissante.
     *
     * <p><b>Exemple :</b> annoncesEnAttenteEnvoiEmailPage(0) retourne la première page des annonces au statut 1 ; les annonces aux statuts 2 à 6 sont exclues.</p>
     *
     * @param pageIndex index de la page (commençant à 0, les valeurs négatives sont ramenées à 0)
     * @return la page d'annonces en attente au format DTO
     */
    public Page<AnnonceListe> annoncesEnAttenteEnvoiEmailPage(int pageIndex) {
        int safePageIndex = Math.max(0, pageIndex);
        var pageable = PageRequest.of(safePageIndex, maxAnnoncesParPage);
        return annonceListeMapper.toAnnonceListePage(
                annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(1, pageable));
    }

    // Met à jour le statut d'une annonce en base.
    private void updateStatusAnnonce(Long annonceId, Integer statusAnnonce) {
        annonceRepository.updateStatusAnnonce(annonceId, statusAnnonce);
    }

    /**
     * Marque l'annonce comme dépassée / sans suite (statut 3).
     *
     * <p><b>Exemple :</b> setDepasse(7L) fait passer l'annonce 7 au statut 3 (dépassée).</p>
     *
     * @param annonceId identifiant de l'annonce
     */
    @Transactional
    public void setDepasse(Long annonceId) {
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
            return annonceListeMapper.toAnnonceListePage(includeArchives
                    ? annonceRepository.findAllOrderByDateEnvoiDesc(pageable)
                    : annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(2, pageable));
        }

        // ✅ Texte présent : recherche multi-champs, et archives=true => tous status
        return annonceListeMapper.toAnnonceListePage(annonceRepository.search(q, includeArchives, pageable));
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
        return annonceListeMapper.toAnnonceListePage(
                annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(5, pageable));
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
