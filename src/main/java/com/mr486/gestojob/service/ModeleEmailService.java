package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.ModeleEmailVue;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.ModeleEmail;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.ModeleEmailRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import com.mr486.gestojob.tools.HtmlSanitizer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des modèles d'email éditables : lecture (avec cache mémoire),
 * mise à jour (nettoyage du HTML), réinitialisation à la valeur par défaut, et
 * amorçage de la base au démarrage à partir des ressources classpath.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModeleEmailService {

    private final ModeleEmailRepository modeleEmailRepository;
    private final UtilisateurCourant utilisateurCourant;
    private final ProfilService profilService;
    // Clé du cache : titulaire.name() + ":" + cle.
    // Cohérence garantie : chaque écriture passe par enregistrer() (instance unique supposée).
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Amorce la base : crée, pour chaque titulaire, les modèles manquants depuis
     * leurs ressources par défaut et remplit le cache mémoire. Idempotent
     * (n'écrase pas l'existant). S'exécute au démarrage, sans utilisateur connecté.
     *
     * <p><b>Exemple :</b> au premier démarrage, insère les 5 modèles de chacun des
     * titulaires ; aux démarrages suivants, ne fait que recharger le cache.</p>
     */
    @Transactional
    public void initialiser() {
        for (Titulaire titulaire : Titulaire.values()) {
            for (CleModele cle : CleModele.values()) {
                ModeleEmail modele = modeleEmailRepository.findByTitulaireAndCle(titulaire, cle.name())
                        .orElseGet(() -> creerDepuisDefaut(titulaire, cle));
                cache.put(cleCache(titulaire, cle.name()), modele.getContenu());
            }
        }
        log.info("modèles d'email initialisés ({} clés x {} titulaires)",
                CleModele.values().length, Titulaire.values().length);
    }

    /**
     * Retourne le contenu courant d'un modèle d'un titulaire.
     *
     * <p><b>Exemple :</b> {@code getContenu(Titulaire.RENEE, "CONTENU_IA")} retourne le
     * HTML du modèle IA de Renée ; une clé inconnue lève MetierException.</p>
     *
     * @param titulaire propriétaire du modèle
     * @param cle       clé du modèle
     * @return le contenu courant
     * @throws MetierException si la clé est inconnue
     */
    public String getContenu(Titulaire titulaire, String cle) {
        String cleDuCache = cleCache(titulaire, cle);
        String contenu = cache.get(cleDuCache);
        if (contenu != null) {
            return contenu;
        }
        CleModele cleModele = cleValide(cle);
        ModeleEmail modele = modeleEmailRepository.findByTitulaireAndCle(titulaire, cle)
                .orElseGet(() -> creerDepuisDefaut(titulaire, cleModele));
        cache.put(cleDuCache, modele.getContenu());
        return modele.getContenu();
    }

    /**
     * Liste les modèles du titulaire courant pour l'affichage dans Paramètres.
     * Pour les corps de lettre, le libellé d'affichage vient du profil.
     *
     * <p><b>Exemple :</b> retourne 5 lignes (3 contenus + 2 libellés) avec leur
     * contenu courant.</p>
     *
     * @return la liste des modèles au format d'affichage
     */
    public List<ModeleEmailVue> listerModeles() {
        Titulaire titulaire = utilisateurCourant.titulaire();
        Profil profil = profilService.getProfilCourant();
        List<ModeleEmailVue> vues = new ArrayList<>();
        for (CleModele cle : CleModele.values()) {
            String libelleUi = cle.getCategorie() == CleModele.Categorie.CONTENU
                    ? profilService.libelleContenu(profil, cle)
                    : cle.getLibelleUi();
            vues.add(ModeleEmailVue.builder()
                    .cle(cle.name())
                    .libelleUi(libelleUi)
                    .html(cle.isHtml())
                    .contenu(getContenu(titulaire, cle.name()))
                    .variables(cle.getVariables())
                    .variablesObligatoires(cle.getVariablesObligatoires())
                    .build());
        }
        return vues;
    }

    /**
     * Met à jour un modèle du titulaire courant. Le HTML est nettoyé ; le texte est
     * conservé tel quel (espaces de tête/fin retirés).
     *
     * <p><b>Exemple :</b> {@code mettreAJour("CONTENU_GENERAL", "<p>x</p><script>")}
     * enregistre « &lt;p&gt;x&lt;/p&gt; » (script retiré).</p>
     *
     * @param cle         clé du modèle
     * @param contenuBrut contenu soumis par l'utilisateur
     * @throws MetierException si la clé est inconnue ou si une variable obligatoire manque
     */
    @Transactional
    public void mettreAJour(String cle, String contenuBrut) {
        CleModele cleModele = cleValide(cle);
        String contenu = cleModele.isHtml()
                ? HtmlSanitizer.nettoie(contenuBrut)
                : (contenuBrut == null ? "" : contenuBrut.trim());
        List<String> manquantes = cleModele.variablesManquantes(contenu);
        if (!manquantes.isEmpty()) {
            log.warn("modèle {} refusé : variables manquantes {}", cle, manquantes);
            throw new MetierException("Modèle non enregistré : variable(s) manquante(s) "
                    + String.join(", ", manquantes) + ".");
        }
        enregistrer(utilisateurCourant.titulaire(), cle, contenu);
        log.info("modèle {} mis à jour", cle);
    }

    /**
     * Réinitialise un modèle du titulaire courant à sa valeur par défaut
     * (ressource classpath de ce titulaire).
     *
     * <p><b>Exemple :</b> {@code reinitialiser("LIBELLE_SPONTANEE")} restaure le
     * libellé d'origine de candidature spontanée.</p>
     *
     * @param cle clé du modèle
     * @throws MetierException si la clé est inconnue
     */
    @Transactional
    public void reinitialiser(String cle) {
        CleModele cleModele = cleValide(cle);
        Titulaire titulaire = utilisateurCourant.titulaire();
        enregistrer(titulaire, cle, lireRessource(cleModele.getCheminRessource(titulaire)));
        log.info("modèle {} réinitialisé", cle);
    }

    // Clé du cache mémoire, propre à chaque titulaire.
    private String cleCache(Titulaire titulaire, String cle) {
        return titulaire.name() + ":" + cle;
    }

    // Crée et persiste un modèle à partir de la ressource par défaut du titulaire.
    private ModeleEmail creerDepuisDefaut(Titulaire titulaire, CleModele cle) {
        ModeleEmail modele = ModeleEmail.builder()
                .cle(cle.name())
                .titulaire(titulaire)
                .contenu(lireRessource(cle.getCheminRessource(titulaire)))
                .build();
        return modeleEmailRepository.save(modele);
    }

    // Enregistre un contenu (insert ou update) pour un titulaire et rafraîchit le cache.
    private void enregistrer(Titulaire titulaire, String cle, String contenu) {
        ModeleEmail modele = modeleEmailRepository.findByTitulaireAndCle(titulaire, cle)
                .orElseGet(() -> ModeleEmail.builder().cle(cle).titulaire(titulaire).build());
        modele.setContenu(contenu);
        modeleEmailRepository.save(modele);
        cache.put(cleCache(titulaire, cle), contenu);
    }

    // Valide la clé contre l'enum (MetierException si inconnue).
    private CleModele cleValide(String cle) {
        try {
            return CleModele.valueOf(cle);
        } catch (IllegalArgumentException ex) {
            throw new MetierException("Modèle inconnu : " + cle, ex);
        }
    }

    // Lit une ressource classpath en UTF-8.
    private String lireRessource(String chemin) {
        try (InputStream is = new ClassPathResource(chemin).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Ressource de modèle introuvable : " + chemin, ex);
        }
    }
}
