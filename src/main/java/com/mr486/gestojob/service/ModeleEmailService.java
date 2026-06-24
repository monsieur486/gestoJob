package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.ModeleEmailVue;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.ModeleEmail;
import com.mr486.gestojob.persistance.ModeleEmailRepository;
import com.mr486.gestojob.tools.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Amorce la base : crée les modèles manquants depuis leurs ressources par
     * défaut et remplit le cache mémoire. Idempotent (n'écrase pas l'existant).
     *
     * <p><b>Exemple :</b> au premier démarrage, insère les 5 modèles par défaut ;
     * aux démarrages suivants, ne fait que recharger le cache.</p>
     */
    @Transactional
    public void initialiser() {
        for (CleModele cle : CleModele.values()) {
            ModeleEmail modele = modeleEmailRepository.findByCle(cle.name())
                    .orElseGet(() -> creerDepuisDefaut(cle));
            cache.put(cle.name(), modele.getContenu());
        }
        log.info("modèles d'email initialisés ({} clés)", CleModele.values().length);
    }

    /**
     * Retourne le contenu courant d'un modèle.
     *
     * <p><b>Exemple :</b> {@code getContenu("CONTENU_IA")} retourne le HTML du modèle
     * IA ; une clé inconnue lève NoSuchElementException.</p>
     *
     * @param cle clé du modèle
     * @return le contenu courant
     * @throws java.util.NoSuchElementException si la clé est inconnue
     */
    public String getContenu(String cle) {
        String contenu = cache.get(cle);
        if (contenu != null) {
            return contenu;
        }
        CleModele cleModele = cleValide(cle);
        ModeleEmail modele = modeleEmailRepository.findByCle(cle)
                .orElseGet(() -> creerDepuisDefaut(cleModele));
        cache.put(cle, modele.getContenu());
        return modele.getContenu();
    }

    /**
     * Liste tous les modèles pour l'affichage dans Paramètres.
     *
     * <p><b>Exemple :</b> retourne 5 lignes (3 contenus + 2 libellés) avec leur
     * contenu courant.</p>
     *
     * @return la liste des modèles au format d'affichage
     */
    public List<ModeleEmailVue> listerModeles() {
        List<ModeleEmailVue> vues = new ArrayList<>();
        for (CleModele cle : CleModele.values()) {
            vues.add(ModeleEmailVue.builder()
                    .cle(cle.name())
                    .libelleUi(cle.getLibelleUi())
                    .html(cle.isHtml())
                    .contenu(getContenu(cle.name()))
                    .variables(cle.getVariables())
                    .build());
        }
        return vues;
    }

    /**
     * Met à jour un modèle. Le HTML est nettoyé ; le texte est conservé tel quel
     * (espaces de tête/fin retirés).
     *
     * <p><b>Exemple :</b> {@code mettreAJour("CONTENU_GENERAL", "<p>x</p><script>")}
     * enregistre « &lt;p&gt;x&lt;/p&gt; » (script retiré).</p>
     *
     * @param cle         clé du modèle
     * @param contenuBrut contenu soumis par l'utilisateur
     * @throws java.util.NoSuchElementException si la clé est inconnue
     */
    @Transactional
    public void mettreAJour(String cle, String contenuBrut) {
        CleModele cleModele = cleValide(cle);
        String contenu = cleModele.isHtml()
                ? HtmlSanitizer.nettoie(contenuBrut)
                : (contenuBrut == null ? "" : contenuBrut.trim());
        enregistrer(cle, contenu);
        log.info("modèle {} mis à jour", cle);
    }

    /**
     * Réinitialise un modèle à sa valeur par défaut (ressource classpath).
     *
     * <p><b>Exemple :</b> {@code reinitialiser("LIBELLE_SPONTANEE")} restaure le
     * libellé d'origine de candidature spontanée.</p>
     *
     * @param cle clé du modèle
     * @throws java.util.NoSuchElementException si la clé est inconnue
     */
    @Transactional
    public void reinitialiser(String cle) {
        CleModele cleModele = cleValide(cle);
        enregistrer(cle, lireRessource(cleModele.getCheminRessource()));
        log.info("modèle {} réinitialisé", cle);
    }

    // Crée et persiste un modèle à partir de sa ressource par défaut.
    private ModeleEmail creerDepuisDefaut(CleModele cle) {
        ModeleEmail modele = ModeleEmail.builder()
                .cle(cle.name())
                .contenu(lireRessource(cle.getCheminRessource()))
                .build();
        return modeleEmailRepository.save(modele);
    }

    // Enregistre un contenu (insert ou update) et rafraîchit le cache.
    private void enregistrer(String cle, String contenu) {
        ModeleEmail modele = modeleEmailRepository.findByCle(cle)
                .orElseGet(() -> ModeleEmail.builder().cle(cle).build());
        modele.setContenu(contenu);
        modeleEmailRepository.save(modele);
        cache.put(cle, contenu);
    }

    // Valide la clé contre l'enum (NoSuchElementException si inconnue).
    private CleModele cleValide(String cle) {
        try {
            return CleModele.valueOf(cle);
        } catch (IllegalArgumentException ex) {
            throw new NoSuchElementException("Modèle inconnu : " + cle);
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
