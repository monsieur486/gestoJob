package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.dto.RechercheAnnonceForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.StatutAnnonce;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import com.mr486.gestojob.tools.HtmlSanitizer;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier de gestion des annonces (candidatures).
 * Gère la création, la consultation, la suppression et la recherche paginée des
 * annonces. Les transitions de statut sont assurées par {@link AnnonceStatutService},
 * le contenu texte par {@link AnnonceTexteService} et l'envoi des emails par
 * {@link AnnonceMailService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnonceService {

    private final AnnonceRepository annonceRepository;
    private final AnnonceListeMapper annonceListeMapper;
    private final UtilisateurCourant utilisateurCourant;
    private final EntrepriseService entrepriseService;
    private final ContactService contactService;
    @Value("${gestojob.max-annonces-par-page:7}")
    private int maxAnnoncesParPage;

    @Value("${gestojob.max-positifs-par-page:7}")
    private int maxPositifsParPage;

    // Récupère toutes les annonces d'une entreprise, triées par statut croissant.
    private List<Annonce> findAllByEntrepriseId(Integer entrepriseId) {
        return annonceRepository.findAllByTitulaireAndEntrepriseIdOrderByStatusAnnonceAsc(
                utilisateurCourant.titulaire(), entrepriseId);
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
        return annonceRepository.findByIdAndTitulaire(annonceId, utilisateurCourant.titulaire()).orElseThrow();
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
    private Annonce save(Annonce a) {
        return annonceRepository.save(a);
    }

    /**
     * Supprime définitivement une annonce.
     *
     * <p><b>Exemple :</b> deleteAnnonce(7L) supprime l'annonce d'id 7 si elle appartient au titulaire courant ; un identifiant inexistant ou appartenant à l'autre titulaire est sans effet (aucune exception levée).</p>
     *
     * @param annonceId identifiant de l'annonce à supprimer
     */
    @Transactional
    public void deleteAnnonce(Long annonceId) {
        annonceRepository.findByIdAndTitulaire(annonceId, utilisateurCourant.titulaire())
                .ifPresent(annonceRepository::delete);
        log.info("annonce supprimée : {}", annonceId);
    }

    /**
     * Crée et enregistre une annonce à partir du formulaire de saisie.
     * Une candidature sans contact (réponse via le site) est marquée comme déjà
     * envoyée (statut 2) avec la date du jour ; sinon elle est mise en attente (statut 1).
     *
     * <p><b>Exemple :</b> un formulaire avec contactId=null est enregistré au statut 2 (envoyé) avec la date du jour ; avec un contactId=3, l'annonce est créée au statut 1 (en attente) et sans date d'envoi.</p>
     *
     * @param form formulaire de l'annonce
     * @throws IllegalArgumentException si aucune entreprise n'est renseignée
     * @throws java.util.NoSuchElementException si l'entreprise ou le contact n'appartient pas au titulaire courant
     */
    @Transactional
    public void saveForm(AnnonceForm form) {
        creer(form, null);
    }

    /**
     * Crée et enregistre une annonce portant sa propre lettre de motivation personnalisée.
     * Mêmes validations et même règle de statut que {@link #saveForm(AnnonceForm)} ;
     * la lettre est nettoyée par {@link HtmlSanitizer#nettoie(String)} puis doit contenir
     * la variable {@code {{POLITESSE}}}.
     *
     * <p><b>Exemple :</b> creerAvecLettre(form, "&lt;p&gt;{{POLITESSE}}&lt;/p&gt;&lt;script&gt;x&lt;/script&gt;") enregistre l'annonce avec la lettre « &lt;p&gt;{{POLITESSE}}&lt;/p&gt; » ; une lettre sans {{POLITESSE}} lève MetierException.</p>
     *
     * @param form        formulaire de l'annonce
     * @param lettreHtml  lettre de motivation en HTML, avant nettoyage
     * @return l'annonce enregistrée
     * @throws MetierException si l'entreprise est absente ou si la lettre ne contient pas {{POLITESSE}}
     * @throws java.util.NoSuchElementException si l'entreprise ou le contact n'appartient pas au titulaire courant
     */
    @Transactional
    public Annonce creerAvecLettre(AnnonceForm form, String lettreHtml) {
        String lettre = HtmlSanitizer.nettoie(lettreHtml);
        if (lettre == null || !lettre.contains("{{POLITESSE}}")) {
            log.warn("création d'annonce refusée : lettre sans la variable {{POLITESSE}}");
            throw new MetierException("Lettre refusée : la variable {{POLITESSE}} est obligatoire.");
        }
        return creer(form, lettre);
    }

    // Valide le formulaire puis enregistre l'annonce (lettre nulle = modèle par défaut).
    private Annonce creer(AnnonceForm form, String lettrePersonnalisee) {
        if (form.getEntrepriseId() == null) {
            throw new MetierException("Entreprise obligatoire pour créer une annonce.");
        }

        Long contactId = form.getContactId();
        boolean site = (contactId == null || contactId == 0L);

        // Cloisonnement : l'entreprise et le contact doivent appartenir au titulaire
        // courant, sinon on réagit comme pour un id inconnu.
        if (!Boolean.TRUE.equals(entrepriseService.existe(form.getEntrepriseId()))) {
            throw new NoSuchElementException("Entreprise introuvable : " + form.getEntrepriseId());
        }
        if (!site) {
            contactService.getContact(contactId);
        }

        Annonce a = Annonce.builder()
                .titulaire(utilisateurCourant.titulaire())
                .entrepriseId(form.getEntrepriseId())
                .contactId(site ? null : contactId)
                .typeAnnonce(form.getTypeAnnonce())
                .typeContenu(form.getContenuId())
                .poste(form.getPoste())
                .reference(form.getReference())
                .lettrePersonnalisee(lettrePersonnalisee)
                .statusAnnonce(site ? StatutAnnonce.EN_COURS.getCode() : StatutAnnonce.BOITE_ENVOI.getCode())
                .dateEnvoi(site ? OffsetDateTime.now() : null)
                .build();

        return save(a);
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
                annonceRepository.findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                        utilisateurCourant.titulaire(), StatutAnnonce.BOITE_ENVOI.getCode(), pageable));
    }

    /**
     * Recherche paginée d'annonces. Sans texte de recherche : retourne soit toutes
     * les annonces (avec archives), soit toutes sauf les archivées (statut 6). Avec
     * un texte : effectue une recherche multi-champs, élargie aux archivées si les
     * archives sont incluses.
     *
     * <p><b>Exemple :</b> sans texte et avecArchives=false, retourne toutes les annonces sauf les archivées (statut 6) ; avec le texte « Java » et avecArchives=true, recherche « Java » sur tous les statuts.</p>
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
            // Pas de texte : avec archives => toutes les annonces ; sinon toutes
            // sauf les archivées (statut 6).
            return annonceListeMapper.toAnnonceListePage(includeArchives
                    ? annonceRepository.findAllOrderByDateEnvoiDesc(utilisateurCourant.titulaire(), pageable)
                    : annonceRepository.findAllByStatusAnnonceNotOrderByDateEnvoiDesc(
                            utilisateurCourant.titulaire(), StatutAnnonce.ARCHIVE.getCode(), pageable));
        }

        // Texte présent : recherche multi-champs ; archives=true => tous les statuts
        return annonceListeMapper.toAnnonceListePage(annonceRepository.search(
                utilisateurCourant.titulaire(), q, includeArchives, pageable));
    }

    /**
     * Retourne le nombre total d'annonces enregistrées.
     *
     * <p><b>Exemple :</b> avec 12 annonces en base, countAnnonces() retourne 12.</p>
     *
     * @return le nombre d'annonces
     */
    public long countAnnonces() {
        return annonceRepository.countByTitulaire(utilisateurCourant.titulaire());
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
                annonceRepository.findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                        utilisateurCourant.titulaire(), StatutAnnonce.POSITIF.getCode(), pageable));
    }
}
