package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.StatutAnnonce;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import com.mr486.gestojob.tools.HtmlSanitizer;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de création des annonces (candidatures), avec ou sans lettre personnalisée.
 * Valide le cloisonnement par titulaire (entreprise et contact) puis applique la règle
 * de statut : sans contact l'annonce est déjà envoyée, avec contact elle est mise en
 * attente. Les autres opérations sont dans {@link AnnonceService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnonceCreationService {

    private final AnnonceRepository annonceRepository;
    private final UtilisateurCourant utilisateurCourant;
    private final EntrepriseService entrepriseService;
    private final ContactService contactService;

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
     * la variable {@code {{POLITESSE}}} (voir {@link #nettoyerLettre(String)}).
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
        return creer(form, nettoyerLettre(lettreHtml));
    }

    /**
     * Nettoie une lettre et vérifie qu'elle contient la variable {@code {{POLITESSE}}}.
     * Sans écriture : utilisable pour refuser une lettre avant toute écriture en base.
     *
     * <p><b>Exemple :</b> nettoyerLettre("&lt;p&gt;{{POLITESSE}}&lt;/p&gt;&lt;script&gt;x&lt;/script&gt;") renvoie « &lt;p&gt;{{POLITESSE}}&lt;/p&gt; » ; nettoyerLettre("&lt;p&gt;Bonjour&lt;/p&gt;") lève MetierException.</p>
     *
     * @param lettreHtml lettre de motivation en HTML, avant nettoyage
     * @return la lettre nettoyée
     * @throws MetierException si la lettre est absente ou ne contient pas {{POLITESSE}} après nettoyage
     */
    public static String nettoyerLettre(String lettreHtml) {
        String lettre = HtmlSanitizer.nettoie(lettreHtml);
        if (lettre == null || !lettre.contains("{{POLITESSE}}")) {
            log.warn("création d'annonce refusée : lettre sans la variable {{POLITESSE}}");
            throw new MetierException("Lettre refusée : la variable {{POLITESSE}} est obligatoire.");
        }
        return lettre;
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

        return annonceRepository.save(a);
    }
}
