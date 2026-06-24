# Modèles d'email éditables — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** permettre de corriger depuis la page Paramètres les 3 corps de lettre (HTML riche : gras + puces) et les 2 libellés d'email (texte), persistés en base, avec texte brut auto-dérivé du HTML.

**Architecture :** un catalogue de modèles (`CleModele`, enum) ; une table `modele_email` (Liquibase) stockant le contenu éditable par clé ; un `ModeleEmailService` (cache mémoire + lecture/écriture/réinitialisation) alimenté au démarrage depuis des ressources par défaut ; `ContenuService` et un nouveau `LibelleService` lisent leurs modèles via ce service ; un éditeur Quill dans `parametres.html`. Le HTML enregistré est nettoyé par jsoup.

**Tech Stack :** Java 17, Spring Boot 4, Spring Data JPA, Liquibase, Thymeleaf, Lombok, jsoup (déjà présent), Quill (CDN), JUnit 5 + Mockito + AssertJ.

## Global Constraints

- **Java 17**, **Maven** via `./mvnw`, **Lombok**.
- **Tout en français** : noms de domaine, libellés UI, Javadoc, commits.
- **Javadoc FR** sur le public : description puis `<p><b>Exemple :</b> …</p>` **avant** les balises `@`. Méthodes privées : court commentaire `//`.
- **Logs SLF4J** (`@Slf4j`, jamais `System.out`) : `info` sur les flux métier publics, `warn`/`error` sur anomalies (exception en dernier argument), messages paramétrés `{}`, aucune donnée sensible.
- **Checkstyle bloquant** (imports explicites, camelCase, lignes ≤ 120) + **JaCoCo ≥ 90 %** : `./mvnw verify` doit rester vert (base lancée via `./dev-start.sh`).
- **Schéma owné par Liquibase** (`ddl-auto: validate`) : toute table passe par un changelog enregistré dans `master.xml`.
- Variables de substitution au format `{{...}}`, substitution **côté serveur**.
- Constantes métier dans `configuration/ApplicationConfiguration`.

---

## File Structure

**Créés :**
- `src/main/java/com/mr486/gestojob/model/CleModele.java` — catalogue des modèles (clé, catégorie, HTML/texte, libellé UI, ressource par défaut, mapping depuis les types).
- `src/main/java/com/mr486/gestojob/model/ModeleEmail.java` — entité JPA (`modele_email`).
- `src/main/java/com/mr486/gestojob/persistance/ModeleEmailRepository.java` — repository Spring Data.
- `src/main/java/com/mr486/gestojob/tools/HtmlSanitizer.java` — nettoyage HTML (jsoup Safelist).
- `src/main/java/com/mr486/gestojob/service/ModeleEmailService.java` — lecture/écriture/réinit + cache + seed.
- `src/main/java/com/mr486/gestojob/service/LibelleService.java` — construction de l'objet d'email.
- `src/main/java/com/mr486/gestojob/dto/ModeleEmailVue.java` — ligne d'affichage Paramètres.
- `src/main/java/com/mr486/gestojob/configuration/ModeleEmailInitializer.java` — runner de seed au démarrage.
- `src/main/resources/db/changelog/007-creation_modele_email.sql` — table.
- `src/main/resources/modeles/defaut/CONTENU_GENERAL.html`, `CONTENU_MICROSERVICES.html`, `CONTENU_IA.html`, `LIBELLE_REFERENCE.txt`, `LIBELLE_SPONTANEE.txt` — valeurs par défaut.
- Tests : `CleModeleTest`, `ModeleEmailServiceTest`, `HtmlSanitizerTest`, `LibelleServiceTest`, `ParametresPageControllerTest`, `AnnonceListeMapperTest`.

**Modifiés :**
- `src/main/resources/db/changelog/master.xml` — enregistre le 007.
- `configuration/ApplicationConfiguration.java` — constantes des postes par défaut.
- `service/ContenuService.java` (+ test) — lit le HTML en base, dérive le texte.
- `service/AnnonceService.java` (+ test) — libellé via `LibelleService`.
- `service/AnnonceMailService.java` (+ test) — objet via `LibelleService`.
- `service/AnnonceListeMapper.java` — libellé via `LibelleService`.
- `model/Annonce.java` (+ test `AnnonceTest`) — retrait de `getLibelle()`.
- `controller/ParametresPageController.java` (+ test) — GET/POST/réinit.
- `templates/parametres.html`, `static/js/main.js` — éditeurs.

**Note de raffinement du spec :** le catalogue (catégorie, format, libellé UI, ressource) vit dans l'enum `CleModele` (cohérent avec `StatutAnnonce`/`TypeContenu`) plutôt qu'en colonnes ; la table ne stocke que `cle` + `contenu`. Le seed se fait au démarrage depuis des ressources classpath (et non par `INSERT` SQL) pour éviter l'échappement des apostrophes et le découpage Liquibase sur les `;` des entités HTML (`&eacute;`).

---

### Task 1 : Catalogue `CleModele` + ressources par défaut

**Files:**
- Create: `src/main/java/com/mr486/gestojob/model/CleModele.java`
- Create: `src/main/resources/modeles/defaut/CONTENU_GENERAL.html`
- Create: `src/main/resources/modeles/defaut/CONTENU_MICROSERVICES.html`
- Create: `src/main/resources/modeles/defaut/CONTENU_IA.html`
- Create: `src/main/resources/modeles/defaut/LIBELLE_REFERENCE.txt`
- Create: `src/main/resources/modeles/defaut/LIBELLE_SPONTANEE.txt`
- Test: `src/test/java/com/mr486/gestojob/model/CleModeleTest.java`

**Interfaces:**
- Produces :
  - `enum CleModele { CONTENU_GENERAL, CONTENU_MICROSERVICES, CONTENU_IA, LIBELLE_REFERENCE, LIBELLE_SPONTANEE }`
  - `enum CleModele.Categorie { CONTENU, LIBELLE }`
  - `Categorie getCategorie()`, `boolean isHtml()`, `String getLibelleUi()`, `String getCheminRessource()`, `String getVariables()`
  - `static CleModele pourTypeContenu(Integer code)`, `static CleModele pourTypeAnnonce(Integer code)`

- [ ] **Step 1 : Créer les ressources par défaut des contenus (copie verbatim)**

Copier **mot pour mot** les corps HTML actuels de `ContenuService.java` dans les fichiers, en conservant les `{{POLITESSE}}`/`{{POSTE}}` :
- `CONTENU_GENERAL.html` ← contenu du text block de `annoncePosteGeneralHtmlTemplate()` (`ContenuService.java:22-33`, sans les guillemets `"""`).
- `CONTENU_MICROSERVICES.html` ← text block de `annoncePosteMicroserviceHtmlTemplate()` (`ContenuService.java:62-75`).
- `CONTENU_IA.html` ← text block de `annoncePosteIaAgentiqueHtmlTemplate()` (`ContenuService.java:106-119`).

- [ ] **Step 2 : Créer les ressources par défaut des libellés**

`LIBELLE_REFERENCE.txt` (une seule ligne, sans saut final) :
```
Réf [{{REFERENCE}}] {{NOM}} - candidature au poste {{POSTE}}
```

`LIBELLE_SPONTANEE.txt` (une seule ligne) :
```
{{NOM}} - Candidature spontanée pour un poste de développeur Java - Springboot
```

- [ ] **Step 3 : Écrire le test de l'enum**

```java
package com.mr486.gestojob.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CleModeleTest {

    @Test
    void pourTypeContenu_mappeLesCodes() {
        assertThat(CleModele.pourTypeContenu(1)).isEqualTo(CleModele.CONTENU_MICROSERVICES);
        assertThat(CleModele.pourTypeContenu(2)).isEqualTo(CleModele.CONTENU_IA);
        assertThat(CleModele.pourTypeContenu(0)).isEqualTo(CleModele.CONTENU_GENERAL);
        assertThat(CleModele.pourTypeContenu(null)).isEqualTo(CleModele.CONTENU_GENERAL);
        assertThat(CleModele.pourTypeContenu(99)).isEqualTo(CleModele.CONTENU_GENERAL);
    }

    @Test
    void pourTypeAnnonce_referenceSinonSpontanee() {
        assertThat(CleModele.pourTypeAnnonce(1)).isEqualTo(CleModele.LIBELLE_REFERENCE);
        assertThat(CleModele.pourTypeAnnonce(0)).isEqualTo(CleModele.LIBELLE_SPONTANEE);
        assertThat(CleModele.pourTypeAnnonce(null)).isEqualTo(CleModele.LIBELLE_SPONTANEE);
    }

    @Test
    void metadonnees_sontCoherentes() {
        assertThat(CleModele.CONTENU_GENERAL.isHtml()).isTrue();
        assertThat(CleModele.CONTENU_GENERAL.getCategorie()).isEqualTo(CleModele.Categorie.CONTENU);
        assertThat(CleModele.LIBELLE_REFERENCE.isHtml()).isFalse();
        assertThat(CleModele.LIBELLE_REFERENCE.getCategorie()).isEqualTo(CleModele.Categorie.LIBELLE);
        assertThat(CleModele.CONTENU_IA.getCheminRessource()).isEqualTo("modeles/defaut/CONTENU_IA.html");
    }
}
```

- [ ] **Step 4 : Lancer le test (échec attendu)**

Run: `./mvnw -q test -Dtest=CleModeleTest`
Expected: échec de compilation (`CleModele` introuvable).

- [ ] **Step 5 : Implémenter `CleModele`**

```java
package com.mr486.gestojob.model;

/**
 * Catalogue des modèles d'email éditables : associe chaque clé stable à sa
 * catégorie, son format, son libellé d'affichage, sa ressource par défaut et les
 * variables substituables. Centralise le mapping depuis les types métier
 * ({@link TypeContenu}, {@link TypeAnnonce}), dans l'esprit des autres enums du
 * domaine (principe ouvert/fermé).
 */
public enum CleModele {

    CONTENU_GENERAL(Categorie.CONTENU, true, "Lettre — général",
            "modeles/defaut/CONTENU_GENERAL.html", "{{POLITESSE}}, {{POSTE}}"),
    CONTENU_MICROSERVICES(Categorie.CONTENU, true, "Lettre — microservices",
            "modeles/defaut/CONTENU_MICROSERVICES.html", "{{POLITESSE}}, {{POSTE}}"),
    CONTENU_IA(Categorie.CONTENU, true, "Lettre — IA agentique",
            "modeles/defaut/CONTENU_IA.html", "{{POLITESSE}}, {{POSTE}}"),
    LIBELLE_REFERENCE(Categorie.LIBELLE, false, "Objet — candidature à une référence",
            "modeles/defaut/LIBELLE_REFERENCE.txt", "{{REFERENCE}}, {{NOM}}, {{POSTE}}"),
    LIBELLE_SPONTANEE(Categorie.LIBELLE, false, "Objet — candidature spontanée",
            "modeles/defaut/LIBELLE_SPONTANEE.txt", "{{NOM}}, {{POSTE}}");

    /** Nature du modèle : corps de lettre ou objet d'email. */
    public enum Categorie { CONTENU, LIBELLE }

    private final Categorie categorie;
    private final boolean html;
    private final String libelleUi;
    private final String cheminRessource;
    private final String variables;

    CleModele(Categorie categorie, boolean html, String libelleUi,
              String cheminRessource, String variables) {
        this.categorie = categorie;
        this.html = html;
        this.libelleUi = libelleUi;
        this.cheminRessource = cheminRessource;
        this.variables = variables;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public boolean isHtml() {
        return html;
    }

    public String getLibelleUi() {
        return libelleUi;
    }

    public String getCheminRessource() {
        return cheminRessource;
    }

    public String getVariables() {
        return variables;
    }

    /**
     * Retourne la clé de contenu correspondant à un code de type de contenu.
     *
     * <p><b>Exemple :</b> {@code pourTypeContenu(1)} retourne CONTENU_MICROSERVICES ;
     * {@code pourTypeContenu(null)} retourne CONTENU_GENERAL.</p>
     *
     * @param code code de {@link TypeContenu} (peut être nul)
     * @return la clé de contenu, CONTENU_GENERAL par défaut
     */
    public static CleModele pourTypeContenu(Integer code) {
        if (code != null && code == TypeContenu.MICROSERVICES.getCode()) {
            return CONTENU_MICROSERVICES;
        }
        if (code != null && code == TypeContenu.IA.getCode()) {
            return CONTENU_IA;
        }
        return CONTENU_GENERAL;
    }

    /**
     * Retourne la clé de libellé correspondant à un code de type d'annonce.
     *
     * <p><b>Exemple :</b> {@code pourTypeAnnonce(1)} retourne LIBELLE_REFERENCE ;
     * tout autre code (ou null) retourne LIBELLE_SPONTANEE.</p>
     *
     * @param code code de {@link TypeAnnonce} (peut être nul)
     * @return la clé de libellé
     */
    public static CleModele pourTypeAnnonce(Integer code) {
        return (code != null && code == TypeAnnonce.REFERENCE.getCode())
                ? LIBELLE_REFERENCE
                : LIBELLE_SPONTANEE;
    }
}
```

- [ ] **Step 6 : Lancer le test (succès attendu)**

Run: `./mvnw -q test -Dtest=CleModeleTest`
Expected: PASS.

- [ ] **Step 7 : Commit**

```bash
git add src/main/java/com/mr486/gestojob/model/CleModele.java \
        src/main/resources/modeles/defaut/ \
        src/test/java/com/mr486/gestojob/model/CleModeleTest.java
git commit -m "feat(modeles-email): catalogue CleModele et modèles par défaut"
```

---

### Task 2 : Migration Liquibase + entité `ModeleEmail` + repository

**Files:**
- Create: `src/main/resources/db/changelog/007-creation_modele_email.sql`
- Modify: `src/main/resources/db/changelog/master.xml`
- Create: `src/main/java/com/mr486/gestojob/model/ModeleEmail.java`
- Create: `src/main/java/com/mr486/gestojob/persistance/ModeleEmailRepository.java`

**Interfaces:**
- Produces :
  - `ModeleEmail` (champs `Long id`, `String cle`, `String contenu` + Lombok getters/setters/builder)
  - `ModeleEmailRepository extends JpaRepository<ModeleEmail, Long>` avec `Optional<ModeleEmail> findByCle(String cle)`

- [ ] **Step 1 : Créer le changelog 007**

`src/main/resources/db/changelog/007-creation_modele_email.sql` :
```sql
CREATE TABLE IF NOT EXISTS modele_email (
id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
cle VARCHAR(64) NOT NULL,
contenu TEXT NOT NULL,
CONSTRAINT pk_modele_email PRIMARY KEY (id),
CONSTRAINT uq_modele_email_cle UNIQUE (cle)
);
```

- [ ] **Step 2 : Enregistrer le changelog dans `master.xml`**

Ajouter, après la ligne du 006 :
```xml
    <include file="db/changelog/007-creation_modele_email.sql"/>
```

- [ ] **Step 3 : Créer l'entité `ModeleEmail`**

```java
package com.mr486.gestojob.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entité JPA représentant un modèle d'email éditable (corps de lettre ou objet),
 * persisté dans la table {@code modele_email} et identifié par une clé stable
 * (voir {@link CleModele}).
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "modele_email")
public class ModeleEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    private String cle;
    @NotNull
    @Column(columnDefinition = "TEXT")
    private String contenu;
}
```

- [ ] **Step 4 : Créer le repository**

```java
package com.mr486.gestojob.persistance;

import com.mr486.gestojob.model.ModeleEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository des modèles d'email.
 */
public interface ModeleEmailRepository extends JpaRepository<ModeleEmail, Long> {

    /**
     * Recherche un modèle par sa clé.
     *
     * <p><b>Exemple :</b> {@code findByCle("CONTENU_IA")} retourne le modèle IA s'il
     * existe, sinon un Optional vide.</p>
     *
     * @param cle clé stable du modèle
     * @return le modèle correspondant, s'il existe
     */
    Optional<ModeleEmail> findByCle(String cle);
}
```

- [ ] **Step 5 : Vérifier la compilation et la migration**

Run: `./dev-start.sh && ./mvnw -q -Dtest=GestoJobApplicationTests test`
Expected: PASS (le contexte démarre, Liquibase applique le 007 sans erreur).

- [ ] **Step 6 : Commit**

```bash
git add src/main/resources/db/changelog/ \
        src/main/java/com/mr486/gestojob/model/ModeleEmail.java \
        src/main/java/com/mr486/gestojob/persistance/ModeleEmailRepository.java
git commit -m "feat(modeles-email): table modele_email, entité et repository"
```

---

### Task 3 : `HtmlSanitizer` (jsoup)

**Files:**
- Create: `src/main/java/com/mr486/gestojob/tools/HtmlSanitizer.java`
- Test: `src/test/java/com/mr486/gestojob/tools/HtmlSanitizerTest.java`

**Interfaces:**
- Produces : `static String HtmlSanitizer.nettoie(String html)`

- [ ] **Step 1 : Écrire le test**

```java
package com.mr486.gestojob.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void conserveLesBalisesAutorisees() {
        String html = "<p>Bonjour <strong>tout</strong> le monde</p><ul><li>un</li></ul>";
        String resultat = HtmlSanitizer.nettoie(html);
        assertThat(resultat).contains("<p>").contains("<strong>").contains("<ul>").contains("<li>");
    }

    @Test
    void retireScriptEtAttributsDangereux() {
        String html = "<p onclick=\"vol()\">x</p><script>alert(1)</script>";
        String resultat = HtmlSanitizer.nettoie(html);
        assertThat(resultat).doesNotContain("script").doesNotContain("onclick");
    }

    @Test
    void preserveLesVariables() {
        String html = "<p>{{POLITESSE}}</p><p>poste {{POSTE}}</p>";
        String resultat = HtmlSanitizer.nettoie(html);
        assertThat(resultat).contains("{{POLITESSE}}").contains("{{POSTE}}");
    }

    @Test
    void entreeNulle_retourneChaineVide() {
        assertThat(HtmlSanitizer.nettoie(null)).isEmpty();
    }
}
```

- [ ] **Step 2 : Lancer le test (échec attendu)**

Run: `./mvnw -q test -Dtest=HtmlSanitizerTest`
Expected: échec de compilation (`HtmlSanitizer` introuvable).

- [ ] **Step 3 : Implémenter `HtmlSanitizer`**

```java
package com.mr486.gestojob.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Nettoyage du HTML des modèles d'email : ne laisse passer qu'un jeu restreint de
 * balises de mise en forme (gras, italique, paragraphes, listes, liens), pour
 * éviter toute injection de HTML/JS dans les emails et normaliser la sortie de
 * l'éditeur.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "strong", "b", "em", "i", "ul", "ol", "li", "a")
            .addAttributes("a", "href", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto");

    private HtmlSanitizer() {
    }

    /**
     * Nettoie un fragment HTML en ne conservant que les balises autorisées.
     *
     * <p><b>Exemple :</b> {@code nettoie("<p onclick=x>a</p><script>b</script>")}
     * retourne « &lt;p&gt;a&lt;/p&gt; » (script et attribut retirés) ; les variables
     * {{...}} sont préservées ; une entrée nulle retourne une chaîne vide.</p>
     *
     * @param html le HTML à nettoyer (peut être nul)
     * @return le HTML nettoyé, ou une chaîne vide si l'entrée est nulle
     */
    public static String nettoie(String html) {
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, "", SAFELIST,
                new Document.OutputSettings().prettyPrint(false));
    }
}
```

- [ ] **Step 4 : Lancer le test (succès attendu)**

Run: `./mvnw -q test -Dtest=HtmlSanitizerTest`
Expected: PASS.

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/com/mr486/gestojob/tools/HtmlSanitizer.java \
        src/test/java/com/mr486/gestojob/tools/HtmlSanitizerTest.java
git commit -m "feat(modeles-email): nettoyage HTML via jsoup (HtmlSanitizer)"
```

---

### Task 4 : `ModeleEmailService` + DTO `ModeleEmailVue` + initialiseur

**Files:**
- Create: `src/main/java/com/mr486/gestojob/dto/ModeleEmailVue.java`
- Create: `src/main/java/com/mr486/gestojob/service/ModeleEmailService.java`
- Create: `src/main/java/com/mr486/gestojob/configuration/ModeleEmailInitializer.java`
- Test: `src/test/java/com/mr486/gestojob/service/ModeleEmailServiceTest.java`

**Interfaces:**
- Consumes : `ModeleEmailRepository.findByCle`, `CleModele`, `HtmlSanitizer.nettoie`
- Produces :
  - `ModeleEmailVue` (`String cle`, `String libelleUi`, `boolean html`, `String contenu`, `String variables`)
  - `ModeleEmailService.getContenu(String cle) : String`
  - `ModeleEmailService.listerModeles() : List<ModeleEmailVue>`
  - `ModeleEmailService.mettreAJour(String cle, String contenuBrut) : void`
  - `ModeleEmailService.reinitialiser(String cle) : void`
  - `ModeleEmailService.initialiser() : void`

- [ ] **Step 1 : Créer le DTO `ModeleEmailVue`**

```java
package com.mr486.gestojob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO d'affichage d'un modèle d'email dans la page Paramètres (clé, libellé,
 * indicateur HTML, contenu courant et variables disponibles).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeleEmailVue {
    private String cle;
    private String libelleUi;
    private boolean html;
    private String contenu;
    private String variables;
}
```

- [ ] **Step 2 : Écrire le test du service**

```java
package com.mr486.gestojob.service;

import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.ModeleEmail;
import com.mr486.gestojob.persistance.ModeleEmailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeleEmailServiceTest {

    @Mock
    private ModeleEmailRepository modeleEmailRepository;

    @InjectMocks
    private ModeleEmailService modeleEmailService;

    @Test
    void initialiser_creeLesModelesManquants_etRemplitLeCache() {
        when(modeleEmailRepository.findByCle(any())).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.initialiser();

        // 5 clés => 5 enregistrements créés depuis les ressources par défaut
        verify(modeleEmailRepository, org.mockito.Mockito.times(5)).save(any(ModeleEmail.class));
        assertThat(modeleEmailService.getContenu("LIBELLE_SPONTANEE"))
                .contains("Candidature spontanée");
    }

    @Test
    void getContenu_cleInconnue_leveNoSuchElement() {
        assertThatThrownBy(() -> modeleEmailService.getContenu("INCONNU"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void mettreAJour_nettoieLeHtml_etMetAJourLeCache() {
        when(modeleEmailRepository.findByCle("CONTENU_GENERAL")).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.mettreAJour("CONTENU_GENERAL",
                "<p>{{POLITESSE}}</p><script>x</script>");

        String courant = modeleEmailService.getContenu("CONTENU_GENERAL");
        assertThat(courant).contains("{{POLITESSE}}").doesNotContain("script");
    }

    @Test
    void mettreAJour_libelle_neNettoiePasLeHtml_maisConserveLeTexte() {
        when(modeleEmailRepository.findByCle("LIBELLE_REFERENCE")).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.mettreAJour("LIBELLE_REFERENCE", "Réf {{REFERENCE}} - {{NOM}}");

        assertThat(modeleEmailService.getContenu("LIBELLE_REFERENCE"))
                .isEqualTo("Réf {{REFERENCE}} - {{NOM}}");
    }

    @Test
    void reinitialiser_restaureLaValeurParDefaut() {
        when(modeleEmailRepository.findByCle("LIBELLE_SPONTANEE")).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.reinitialiser("LIBELLE_SPONTANEE");

        assertThat(modeleEmailService.getContenu("LIBELLE_SPONTANEE"))
                .contains("Candidature spontanée");
    }

    @Test
    void listerModeles_retourneLesCinqModeles() {
        when(modeleEmailRepository.findByCle(any())).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        modeleEmailService.initialiser();

        assertThat(modeleEmailService.listerModeles()).hasSize(5);
    }
}
```

- [ ] **Step 3 : Lancer le test (échec attendu)**

Run: `./mvnw -q test -Dtest=ModeleEmailServiceTest`
Expected: échec de compilation (`ModeleEmailService` introuvable).

- [ ] **Step 4 : Implémenter `ModeleEmailService`**

```java
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
        try {
            byte[] octets = new ClassPathResource(chemin).getInputStream().readAllBytes();
            return new String(octets, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Ressource de modèle introuvable : " + chemin, ex);
        }
    }
}
```

- [ ] **Step 5 : Lancer le test (succès attendu)**

Run: `./mvnw -q test -Dtest=ModeleEmailServiceTest`
Expected: PASS.

- [ ] **Step 6 : Créer l'initialiseur de démarrage**

```java
package com.mr486.gestojob.configuration;

import com.mr486.gestojob.service.ModeleEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Amorce les modèles d'email au démarrage de l'application (création des modèles
 * manquants à partir de leurs valeurs par défaut, et chargement du cache).
 */
@Component
@RequiredArgsConstructor
public class ModeleEmailInitializer implements ApplicationRunner {

    private final ModeleEmailService modeleEmailService;

    @Override
    public void run(ApplicationArguments args) {
        modeleEmailService.initialiser();
    }
}
```

- [ ] **Step 7 : Vérifier le démarrage du contexte**

Run: `./mvnw -q -Dtest=GestoJobApplicationTests test`
Expected: PASS (le contexte démarre, les 5 modèles sont seedés).

- [ ] **Step 8 : Commit**

```bash
git add src/main/java/com/mr486/gestojob/dto/ModeleEmailVue.java \
        src/main/java/com/mr486/gestojob/service/ModeleEmailService.java \
        src/main/java/com/mr486/gestojob/configuration/ModeleEmailInitializer.java \
        src/test/java/com/mr486/gestojob/service/ModeleEmailServiceTest.java
git commit -m "feat(modeles-email): service de modèles (cache, seed, maj, réinit)"
```

---

### Task 5 : `LibelleService`

**Files:**
- Create: `src/main/java/com/mr486/gestojob/service/LibelleService.java`
- Test: `src/test/java/com/mr486/gestojob/service/LibelleServiceTest.java`

**Interfaces:**
- Consumes : `ModeleEmailService.getContenu`, `CleModele.pourTypeAnnonce`, `ApplicationConfiguration.CANDIDAT_NOM`
- Produces : `LibelleService.construitLibelle(Annonce annonce) : String`

- [ ] **Step 1 : Écrire le test**

```java
package com.mr486.gestojob.service;

import com.mr486.gestojob.model.Annonce;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibelleServiceTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    @InjectMocks
    private LibelleService libelleService;

    @Test
    void construitLibelle_reference_substitueLesVariables() {
        when(modeleEmailService.getContenu("LIBELLE_REFERENCE"))
                .thenReturn("Réf [{{REFERENCE}}] {{NOM}} - poste {{POSTE}}");
        Annonce annonce = Annonce.builder()
                .typeAnnonce(1).reference("ABC123").poste("Développeur").build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Réf [ABC123] Laurent Touret - poste Développeur");
    }

    @Test
    void construitLibelle_spontanee_utiliseLeModeleSpontanee() {
        when(modeleEmailService.getContenu("LIBELLE_SPONTANEE"))
                .thenReturn("{{NOM}} - spontanée");
        Annonce annonce = Annonce.builder().typeAnnonce(0).build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Laurent Touret - spontanée");
    }

    @Test
    void construitLibelle_referenceNulle_remplaceParChaineVide() {
        when(modeleEmailService.getContenu("LIBELLE_REFERENCE"))
                .thenReturn("Réf [{{REFERENCE}}] {{POSTE}}");
        Annonce annonce = Annonce.builder().typeAnnonce(1).build();

        String libelle = libelleService.construitLibelle(annonce);

        assertThat(libelle).isEqualTo("Réf [] ");
    }
}
```

- [ ] **Step 2 : Lancer le test (échec attendu)**

Run: `./mvnw -q test -Dtest=LibelleServiceTest`
Expected: échec de compilation (`LibelleService` introuvable).

- [ ] **Step 3 : Implémenter `LibelleService`**

```java
package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.CleModele;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Construit l'objet (libellé) d'un email de candidature à partir du modèle de
 * libellé adapté au type d'annonce, en substituant la référence, le nom du
 * candidat et le poste. Isolé de l'entité {@link Annonce} (qui ne doit pas
 * dépendre de la base).
 */
@Service
@RequiredArgsConstructor
public class LibelleService {

    private final ModeleEmailService modeleEmailService;

    /**
     * Construit le libellé d'une annonce.
     *
     * <p><b>Exemple :</b> pour une annonce de type référence (reference=« ABC123 »,
     * poste=« Développeur »), retourne « Réf [ABC123] Laurent Touret - candidature
     * au poste Développeur » ; pour une spontanée, le libellé spontané.</p>
     *
     * @param annonce l'annonce concernée
     * @return le libellé prêt à l'emploi (variables substituées)
     */
    public String construitLibelle(Annonce annonce) {
        CleModele cle = CleModele.pourTypeAnnonce(annonce.getTypeAnnonce());
        return modeleEmailService.getContenu(cle.name())
                .replace("{{REFERENCE}}", valeur(annonce.getReference()))
                .replace("{{NOM}}", ApplicationConfiguration.CANDIDAT_NOM)
                .replace("{{POSTE}}", valeur(annonce.getPoste()));
    }

    // Remplace une valeur nulle par une chaîne vide pour éviter « null » dans le libellé.
    private String valeur(String valeur) {
        return valeur == null ? "" : valeur;
    }
}
```

- [ ] **Step 4 : Lancer le test (succès attendu)**

Run: `./mvnw -q test -Dtest=LibelleServiceTest`
Expected: PASS.

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/com/mr486/gestojob/service/LibelleService.java \
        src/test/java/com/mr486/gestojob/service/LibelleServiceTest.java
git commit -m "feat(modeles-email): LibelleService pour l'objet d'email"
```

---

### Task 6 : Brancher le libellé sur les 3 consommateurs + retirer `Annonce.getLibelle()`

**Files:**
- Modify: `service/AnnonceMailService.java` (+ `AnnonceMailServiceTest.java`)
- Modify: `service/AnnonceService.java` (+ `AnnonceServiceTest.java`)
- Modify: `service/AnnonceListeMapper.java`
- Create: `src/test/java/com/mr486/gestojob/service/AnnonceListeMapperTest.java`
- Modify: `model/Annonce.java` (+ `model/AnnonceTest.java`)

**Interfaces:**
- Consumes : `LibelleService.construitLibelle(Annonce)`
- Produces : (aucune nouvelle signature publique ; `Annonce.getLibelle()` est supprimée)

- [ ] **Step 1 : Injecter `LibelleService` dans `AnnonceMailService` et l'utiliser pour l'objet**

Dans `AnnonceMailService`, ajouter le champ `private final LibelleService libelleService;` (après `mailTools`) et, dans `sendMail`, remplacer :
```java
        String subject = annonce.getLibelle();
```
par :
```java
        String subject = libelleService.construitLibelle(annonce);
```

- [ ] **Step 2 : Adapter `AnnonceMailServiceTest`**

Ajouter le mock et, là où le test vérifie l'envoi, stubber le libellé. En tête de classe :
```java
    @Mock
    private LibelleService libelleService;
```
Dans le(s) test(s) qui appellent `sendDirectEmail`/`sendEmailForPendingAnnonces` jusqu'à l'envoi, ajouter :
```java
        when(libelleService.construitLibelle(org.mockito.ArgumentMatchers.any()))
                .thenReturn("objet de test");
```
(et vérifier au besoin que `mailTools.sendHtmlMail` est appelé avec `"objet de test"`).

- [ ] **Step 3 : Injecter `LibelleService` dans `AnnonceService`**

Ajouter le champ `private final LibelleService libelleService;` (après `annonceListeMapper`). Dans `getAnnonceTxtContenuById`, remplacer :
```java
        String result = annonce.getLibelle() + "\n\n";
```
par :
```java
        String result = libelleService.construitLibelle(annonce) + "\n\n";
```

- [ ] **Step 4 : Adapter `AnnonceServiceTest`**

Ajouter `@Mock private LibelleService libelleService;`. Dans le test de `getAnnonceTxtContenuById`, stubber :
```java
        when(libelleService.construitLibelle(org.mockito.ArgumentMatchers.any()))
                .thenReturn("Objet");
```
et adapter l'assertion (le résultat commence par `"Objet\n\n"`).

- [ ] **Step 5 : Injecter `LibelleService` dans `AnnonceListeMapper`**

Ajouter `private final LibelleService libelleService;` et, dans `fromEntity`, remplacer :
```java
        liste.setLibelle(annonce.getLibelle());
```
par :
```java
        liste.setLibelle(libelleService.construitLibelle(annonce));
```

- [ ] **Step 6 : Écrire un test pour `AnnonceListeMapper`**

```java
package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Entreprise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnonceListeMapperTest {

    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private ContactService contactService;
    @Mock
    private LibelleService libelleService;

    @InjectMocks
    private AnnonceListeMapper mapper;

    @Test
    void toAnnonceListe_construitLeLibelleViaLeService() {
        Annonce annonce = Annonce.builder().id(1L).entrepriseId(3).typeAnnonce(0).build();
        when(entrepriseService.getEntreprisesByIds(anyCollection()))
                .thenReturn(Map.of(3, Entreprise.builder().id(3).nom("Acme").build()));
        when(contactService.getContactsByIds(anyCollection())).thenReturn(Map.of());
        when(libelleService.construitLibelle(any())).thenReturn("Mon objet");

        List<AnnonceListe> resultat = mapper.toAnnonceListe(List.of(annonce));

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getLibelle()).isEqualTo("Mon objet");
    }
}
```

- [ ] **Step 7 : Retirer `Annonce.getLibelle()` et son test**

Dans `model/Annonce.java`, supprimer la méthode `getLibelle()` (lignes 62-81) et l'import devenu inutile `com.mr486.gestojob.configuration.ApplicationConfiguration` s'il n'est plus référencé.
Dans `model/AnnonceTest.java`, supprimer les tests qui appellent `getLibelle()`.

- [ ] **Step 8 : Lancer les tests impactés**

Run: `./mvnw -q test -Dtest=AnnonceMailServiceTest,AnnonceServiceTest,AnnonceListeMapperTest,AnnonceTest`
Expected: PASS (compilation OK, plus aucune référence à `getLibelle`).

- [ ] **Step 9 : Commit**

```bash
git add src/main/java/com/mr486/gestojob/service/AnnonceMailService.java \
        src/main/java/com/mr486/gestojob/service/AnnonceService.java \
        src/main/java/com/mr486/gestojob/service/AnnonceListeMapper.java \
        src/main/java/com/mr486/gestojob/model/Annonce.java \
        src/test/java/com/mr486/gestojob/service/AnnonceMailServiceTest.java \
        src/test/java/com/mr486/gestojob/service/AnnonceServiceTest.java \
        src/test/java/com/mr486/gestojob/service/AnnonceListeMapperTest.java \
        src/test/java/com/mr486/gestojob/model/AnnonceTest.java
git commit -m "refactor(modeles-email): libellé via LibelleService, retire Annonce.getLibelle"
```

---

### Task 7 : Refactor `ContenuService` (lecture base + dérivation texte)

**Files:**
- Modify: `configuration/ApplicationConfiguration.java`
- Modify: `service/ContenuService.java`
- Modify (réécriture): `src/test/java/com/mr486/gestojob/service/ContenuServiceTest.java`

**Interfaces:**
- Consumes : `ModeleEmailService.getContenu`, `HtmlConverterService.htmlToPlainText`, `CleModele.pourTypeContenu`
- Produces : signatures inchangées — `getHtmlContenu(String, Integer, String)`, `getTextContenu(String, Integer, String)`

- [ ] **Step 1 : Ajouter les postes par défaut dans `ApplicationConfiguration`**

```java
    /** Intitulé de poste par défaut, modèle généraliste. */
    public static final String POSTE_DEFAUT_GENERAL = "de développeur Java";

    /** Intitulé de poste par défaut, modèle microservices. */
    public static final String POSTE_DEFAUT_MICROSERVICES = "de développeur Java orienté microservices";

    /** Intitulé de poste par défaut, modèle IA agentique. */
    public static final String POSTE_DEFAUT_IA = "de développeur Java back-end orienté IA agentique";
```

- [ ] **Step 2 : Réécrire `ContenuServiceTest` (avec `ModeleEmailService` mocké)**

```java
package com.mr486.gestojob.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ContenuServiceTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    private ContenuService contenuService;

    @BeforeEach
    void setUp() {
        contenuService = new ContenuService(new HtmlConverterService(), modeleEmailService);
        lenient().when(modeleEmailService.getContenu("CONTENU_GENERAL"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} chez nous</p>");
        lenient().when(modeleEmailService.getContenu("CONTENU_MICROSERVICES"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} en microservices</p>");
        lenient().when(modeleEmailService.getContenu("CONTENU_IA"))
                .thenReturn("<p>{{POLITESSE}}</p><p>poste {{POSTE}} avec des agents IA</p>");
    }

    @Test
    void html_echappeLePosteSaisiParUtilisateur() {
        String html = contenuService.getHtmlContenu("<script>alert(1)</script>", 0, "Madame, Monsieur,");
        assertThat(html).contains("&lt;script&gt;").doesNotContain("<script>");
    }

    @Test
    void html_echappeLaFormuleDePolitesse() {
        String html = contenuService.getHtmlContenu("Développeur", 0, "Madame <b>Durand</b>,");
        assertThat(html).contains("Madame &lt;b&gt;Durand&lt;/b&gt;,").doesNotContain("<b>Durand</b>");
    }

    @Test
    void texte_nEchappePas_carCeNestPasDuHtml() {
        String txt = contenuService.getTextContenu("Dév & Co", 0, "Madame, Monsieur,");
        assertThat(txt).contains("Dév & Co").doesNotContain("&amp;");
    }

    @Test
    void html_utiliseLePosteParDefaut_siVide() {
        String html = contenuService.getHtmlContenu("", 0, "Madame, Monsieur,");
        // htmlEscape produit les entités nommées (é -> &eacute;)
        assertThat(html).contains("de d&eacute;veloppeur Java");
    }

    @Test
    void texte_utiliseLePosteParDefaut_siVide() {
        String txt = contenuService.getTextContenu("", 0, "Madame, Monsieur,");
        assertThat(txt).contains("de développeur Java");
    }

    @Test
    void html_templateMicroservice_siTypeContenu1() {
        assertThat(contenuService.getHtmlContenu("Dev", 1, "Madame, Monsieur,")).contains("microservices");
    }

    @Test
    void html_templateIaAgentique_siTypeContenu2() {
        assertThat(contenuService.getHtmlContenu("Dev", 2, "Madame, Monsieur,")).contains("agents IA");
    }

    @Test
    void texte_typeContenuNull_utiliseLeTemplateGeneral_sansNpe() {
        assertThat(contenuService.getTextContenu("", null, "Madame, Monsieur,")).contains("de développeur Java");
    }

    @Test
    void texte_politesseNull_utiliseLaSalutationGenerique_sansNpe() {
        String txt = contenuService.getTextContenu("Dev", 0, null);
        assertThat(txt).contains("Madame, Monsieur,");
    }

    @Test
    void texte_deriveDuHtml_substitueLaPolitesse() {
        String txt = contenuService.getTextContenu("Dev", 0, "Madame Durand,");
        assertThat(txt).contains("Madame Durand,").doesNotContain("{{POLITESSE}}");
    }
}
```

- [ ] **Step 3 : Lancer le test (échec attendu)**

Run: `./mvnw -q test -Dtest=ContenuServiceTest`
Expected: échec de compilation (constructeur à 2 arguments inexistant).

- [ ] **Step 4 : Refactorer `ContenuService`**

Remplacer tout le corps de la classe par la version lisant les modèles. Supprimer les 6 méthodes de template (`annoncePoste*HtmlTemplate`/`*TxtTemplate`). Nouveau contenu :
```java
package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.CleModele;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Service de génération des contenus de candidature (lettres de motivation).
 * Produit, selon le type de poste, la version HTML et la version texte du message
 * en substituant la formule de politesse et l'intitulé du poste dans le modèle
 * éditable correspondant (lu via {@link ModeleEmailService}). Le texte est dérivé
 * du HTML (source unique).
 */
@Service
@RequiredArgsConstructor
public class ContenuService {

    private final HtmlConverterService htmlConverterService;
    private final ModeleEmailService modeleEmailService;

    /**
     * Génère la version HTML du contenu de la candidature.
     *
     * <p><b>Exemple :</b> un poste contenant « &lt;script&gt; » est échappé dans le
     * HTML produit.</p>
     *
     * @param poste              intitulé du poste (valeur par défaut si vide)
     * @param typeContenu        type de contenu (1 = microservices, 2 = IA, sinon général)
     * @param messageDePolitesse formule de politesse (salutation générique si null)
     * @return le contenu HTML
     */
    public String getHtmlContenu(String poste, Integer typeContenu, String messageDePolitesse) {
        return getContent(poste, typeContenu, messageDePolitesse, true);
    }

    /**
     * Génère la version texte du contenu de la candidature (dérivée du HTML).
     *
     * <p><b>Exemple :</b> avec typeContenu=1 et un poste vide, le texte utilise le
     * poste par défaut « de développeur Java orienté microservices » ; le poste
     * fourni n'est pas échappé.</p>
     *
     * @param poste              intitulé du poste (valeur par défaut si vide)
     * @param typeContenu        type de contenu (1 = microservices, 2 = IA, sinon général)
     * @param messageDePolitesse formule de politesse (salutation générique si null)
     * @return le contenu texte
     */
    public String getTextContenu(String poste, Integer typeContenu, String messageDePolitesse) {
        return getContent(poste, typeContenu, messageDePolitesse, false);
    }

    // Lit le modèle HTML correspondant au type, puis substitue politesse et poste.
    // HTML : saisies échappées (anti-injection). Texte : modèle converti en texte
    // brut (HtmlToPlainText) puis substitution non échappée.
    private String getContent(String poste, Integer typeContenu, String messageDePolitesse, boolean isHtml) {
        CleModele cle = CleModele.pourTypeContenu(typeContenu);
        String modeleHtml = modeleEmailService.getContenu(cle.name());
        String politesse = (messageDePolitesse == null)
                ? ApplicationConfiguration.SALUTATION_GENERIQUE
                : messageDePolitesse;
        String posteDefaut = posteParDefaut(cle);

        if (isHtml) {
            String safePoste = (poste == null || poste.isEmpty()) ? posteDefaut : poste;
            return modeleHtml
                    .replace("{{POLITESSE}}", HtmlUtils.htmlEscape(politesse))
                    .replace("{{POSTE}}", HtmlUtils.htmlEscape(safePoste));
        }

        String texte = htmlConverterService.htmlToPlainText(modeleHtml);
        String posteTexte = (poste == null || poste.isEmpty()) ? posteDefaut : poste;
        return texte
                .replace("{{POLITESSE}}", politesse)
                .replace("{{POSTE}}", posteTexte);
    }

    // Intitulé de poste par défaut selon le modèle.
    private String posteParDefaut(CleModele cle) {
        return switch (cle) {
            case CONTENU_MICROSERVICES -> ApplicationConfiguration.POSTE_DEFAUT_MICROSERVICES;
            case CONTENU_IA -> ApplicationConfiguration.POSTE_DEFAUT_IA;
            default -> ApplicationConfiguration.POSTE_DEFAUT_GENERAL;
        };
    }
}
```

- [ ] **Step 5 : Lancer le test (succès attendu)**

Run: `./mvnw -q test -Dtest=ContenuServiceTest`
Expected: PASS.

- [ ] **Step 6 : Commit**

```bash
git add src/main/java/com/mr486/gestojob/configuration/ApplicationConfiguration.java \
        src/main/java/com/mr486/gestojob/service/ContenuService.java \
        src/test/java/com/mr486/gestojob/service/ContenuServiceTest.java
git commit -m "refactor(modeles-email): ContenuService lit les modèles en base"
```

---

### Task 8 : `ParametresPageController` (GET / POST / réinit)

**Files:**
- Modify: `controller/ParametresPageController.java`
- Test: `src/test/java/com/mr486/gestojob/controller/ParametresPageControllerTest.java`

**Interfaces:**
- Consumes : `ModeleEmailService.listerModeles`, `mettreAJour`, `reinitialiser`
- Produces :
  - `GET /parametres` → vue `parametres` (attribut `modeles`)
  - `POST /parametres/modeles/{cle}` (param `contenu`) → `redirect:/parametres`
  - `POST /parametres/modeles/{cle}/reinitialiser` → `redirect:/parametres`

- [ ] **Step 1 : Écrire le test**

```java
package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.ModeleEmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametresPageControllerTest {

    @Mock
    private ModeleEmailService modeleEmailService;

    @InjectMocks
    private ParametresPageController controller;

    @Test
    void parametres_alimenteLesModeles() {
        when(modeleEmailService.listerModeles()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String vue = controller.parametresView(model);

        assertThat(vue).isEqualTo("parametres");
        assertThat(model.getAttribute("modeles")).isNotNull();
        assertThat(model.getAttribute("page_active")).isEqualTo("parametres");
    }

    @Test
    void enregistrer_metAJour_etRedirige() {
        String vue = controller.enregistrerModele("CONTENU_GENERAL", "<p>x</p>",
                new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("redirect:/parametres");
        verify(modeleEmailService).mettreAJour("CONTENU_GENERAL", "<p>x</p>");
    }

    @Test
    void enregistrer_exposeLErreur_siServiceLeve() {
        doThrow(new RuntimeException("boum")).when(modeleEmailService)
                .mettreAJour("CONTENU_GENERAL", "x");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String vue = controller.enregistrerModele("CONTENU_GENERAL", "x", redirect);

        assertThat(vue).isEqualTo("redirect:/parametres");
        assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo("boum");
    }

    @Test
    void reinitialiser_appelleLeService_etRedirige() {
        String vue = controller.reinitialiserModele("CONTENU_IA", new RedirectAttributesModelMap());

        assertThat(vue).isEqualTo("redirect:/parametres");
        verify(modeleEmailService).reinitialiser("CONTENU_IA");
    }
}
```

- [ ] **Step 2 : Lancer le test (échec attendu)**

Run: `./mvnw -q test -Dtest=ParametresPageControllerTest`
Expected: échec de compilation (méthodes inexistantes).

- [ ] **Step 3 : Implémenter le contrôleur**

```java
package com.mr486.gestojob.controller;

import com.mr486.gestojob.service.ModeleEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Contrôleur MVC de la page des paramètres : affichage et édition des modèles
 * d'email (corps de lettre et libellés).
 */
@Controller
@RequiredArgsConstructor
public class ParametresPageController {

    private final ModeleEmailService modeleEmailService;

    /**
     * Affiche la page des paramètres avec les modèles éditables.
     *
     * <p><b>Exemple :</b> GET /parametres place la liste des modèles dans le modèle
     * et retourne la vue {@code parametres}.</p>
     *
     * @param model le modèle Thymeleaf
     * @return le nom de la vue {@code parametres}
     */
    @GetMapping("/parametres")
    public String parametresView(Model model) {
        model.addAttribute("page_active", "parametres");
        model.addAttribute("modeles", modeleEmailService.listerModeles());
        return "parametres";
    }

    /**
     * Enregistre la nouvelle valeur d'un modèle puis redirige vers Paramètres.
     *
     * <p><b>Exemple :</b> POST /parametres/modeles/CONTENU_GENERAL met à jour le
     * modèle puis redirige ; en cas d'erreur, un message flash est exposé.</p>
     *
     * @param cle                clé du modèle
     * @param contenu            nouveau contenu soumis
     * @param redirectAttributes attributs flash
     * @return une redirection vers {@code /parametres}
     */
    @PostMapping("/parametres/modeles/{cle}")
    public String enregistrerModele(@PathVariable String cle,
                                    @RequestParam("contenu") String contenu,
                                    RedirectAttributes redirectAttributes) {
        try {
            modeleEmailService.mettreAJour(cle, contenu);
            redirectAttributes.addFlashAttribute("successMessage", "Modèle enregistré.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/parametres";
    }

    /**
     * Réinitialise un modèle à sa valeur par défaut puis redirige vers Paramètres.
     *
     * <p><b>Exemple :</b> POST /parametres/modeles/CONTENU_IA/reinitialiser restaure
     * le modèle IA d'origine puis redirige.</p>
     *
     * @param cle                clé du modèle
     * @param redirectAttributes attributs flash
     * @return une redirection vers {@code /parametres}
     */
    @PostMapping("/parametres/modeles/{cle}/reinitialiser")
    public String reinitialiserModele(@PathVariable String cle,
                                      RedirectAttributes redirectAttributes) {
        try {
            modeleEmailService.reinitialiser(cle);
            redirectAttributes.addFlashAttribute("successMessage", "Modèle réinitialisé.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/parametres";
    }
}
```

- [ ] **Step 4 : Lancer le test (succès attendu)**

Run: `./mvnw -q test -Dtest=ParametresPageControllerTest`
Expected: PASS.

- [ ] **Step 5 : Commit**

```bash
git add src/main/java/com/mr486/gestojob/controller/ParametresPageController.java \
        src/test/java/com/mr486/gestojob/controller/ParametresPageControllerTest.java
git commit -m "feat(modeles-email): endpoints Paramètres (affichage, maj, réinit)"
```

---

### Task 9 : Vue `parametres.html` (Quill) + vérification finale

**Files:**
- Modify: `src/main/resources/templates/parametres.html`
- Modify: `src/main/resources/static/js/main.js`

**Interfaces:**
- Consumes : attribut `modeles` (`List<ModeleEmailVue>`), flash `successMessage`/`errorMessage`

- [ ] **Step 1 : Réécrire `parametres.html`**

```html
<!DOCTYPE HTML>
<html lang="fr" xmlns="http://www.w3.org/1999/xhtml" xmlns:th="http://www.thymeleaf.org">
<head>
    <title>GestoJob</title>
    <div th:replace="~{fragments/header :: header-css}"></div>
    <link href="https://cdn.jsdelivr.net/npm/quill@2/dist/quill.snow.css" rel="stylesheet"/>
</head>
<body>
<div th:replace="~{fragments/menu :: menu}"></div>
<div class="container-fluid">
    <div class="alert alert-success" th:if="${successMessage != null}" th:text="${successMessage}">OK</div>
    <div class="alert alert-danger" th:if="${errorMessage != null}" th:text="${errorMessage}">Erreur</div>
    <h3>Paramètres — modèles d'email</h3>

    <div class="card mb-4" th:each="modele : ${modeles}">
        <div class="card-header" th:text="${modele.libelleUi}">Modèle</div>
        <div class="card-body">
            <form method="post" th:action="@{'/parametres/modeles/' + ${modele.cle}}">
                <!-- Contenu HTML : éditeur Quill + champ caché rempli à la soumission -->
                <div th:if="${modele.html}">
                    <div class="editeur-modele" th:attr="data-cible='champ_' + ${modele.cle}"
                         th:utext="${modele.contenu}"></div>
                    <input type="hidden" th:id="'champ_' + ${modele.cle}" name="contenu"
                           th:value="${modele.contenu}"/>
                </div>
                <!-- Libellé : texte brut -->
                <div th:unless="${modele.html}">
                    <input type="text" class="form-control" name="contenu" th:value="${modele.contenu}"/>
                </div>
                <small class="text-muted">Variables : <span th:text="${modele.variables}"></span></small>
                <div class="mt-2">
                    <button type="submit" class="btn btn-primary btn-sm">Enregistrer</button>
                </div>
            </form>
            <form method="post" class="mt-2"
                  th:action="@{'/parametres/modeles/' + ${modele.cle} + '/reinitialiser'}">
                <button type="submit" class="btn btn-outline-secondary btn-sm">Réinitialiser</button>
            </form>
        </div>
    </div>
</div>

<div th:replace="~{fragments/footer :: footer}"></div>
<script src="https://cdn.jsdelivr.net/npm/quill@2/dist/quill.js"></script>
<script src="" th:src="@{/js/main.js}" type="text/javascript"></script>
</body>
</html>
```

> **Sécurité (SRI) :** ajouter `integrity="sha384-…" crossorigin="anonymous"` sur les
> deux balises CDN Quill (le `<link>` CSS et le `<script>`), pour se prémunir d'une
> compromission du CDN. Récupérer les empreintes via le bouton « SRI » de la page
> jsdelivr du fichier (ou `curl -s <url> | openssl dgst -sha384 -binary | openssl base64 -A`).
> Idéalement, faire de même pour le Chart.js déjà chargé dans les autres vues.

- [ ] **Step 2 : Ajouter l'initialisation Quill dans `main.js`**

Ajouter à la fin de `src/main/resources/static/js/main.js` :
```javascript
// Éditeurs Quill des modèles d'email : barre limitée au gras et aux listes à puce.
// Le HTML de chaque éditeur est recopié dans le champ caché ciblé à la soumission.
document.querySelectorAll('.editeur-modele').forEach(function (zone) {
    var editeur = new Quill(zone, {
        theme: 'snow',
        modules: {toolbar: [['bold'], [{list: 'bullet'}]]}
    });
    var champ = document.getElementById(zone.getAttribute('data-cible'));
    var form = zone.closest('form');
    if (form && champ) {
        form.addEventListener('submit', function () {
            champ.value = editeur.root.innerHTML;
        });
    }
});
```

- [ ] **Step 3 : Vérification manuelle**

```bash
./dev-start.sh
./mvnw spring-boot:run
```
Ouvrir `http://localhost:8082/parametres` (connexion `utilisateur` / `Mdp12345*`) :
- les 3 contenus s'affichent dans des éditeurs Quill (gras + puce), les 2 libellés en champ texte ;
- modifier un contenu (ex. mettre un mot en gras + une puce), **Enregistrer** → message de succès, modification persistée après rechargement ;
- **Réinitialiser** restaure la valeur d'origine ;
- envoyer un email de test depuis la file et vérifier le rendu HTML et texte.

Arrêter avec Ctrl+C puis `./dev-stop.sh` (ou laisser tourner pour la suite).

- [ ] **Step 4 : Vérification complète (build + couverture)**

Run (base lancée) : `./mvnw clean verify`
Expected: `BUILD SUCCESS`, Checkstyle 0 violation, JaCoCo « All coverage checks have been met ».

- [ ] **Step 5 : Commit**

```bash
git add src/main/resources/templates/parametres.html \
        src/main/resources/static/js/main.js
git commit -m "feat(modeles-email): page Paramètres avec éditeurs Quill"
```

- [ ] **Step 6 : Site Maven (rapports)**

Run : `./mvnw site`
Expected: `BUILD SUCCESS`, `target/site/` régénéré (Javadoc FR des nouvelles classes incluse).

---

## Self-Review (rempli pendant la rédaction)

- **Couverture du spec :** stockage (Task 2), variables `{{...}}` (Tasks 1/5/7), `ModeleEmailService` + dérivation texte (Tasks 4/7), refactor libellé (Tasks 5/6), nettoyage jsoup (Task 3), UI Quill + réinit (Tasks 8/9), tests ≥ 90 % (chaque task). Raffinement assumé : catalogue en enum + seed par ressources (documenté dans « File Structure »).
- **Placeholders :** aucun TODO/TBD ; tout le code est fourni, sauf les corps HTML par défaut volumineux dont la copie verbatim est référencée par `fichier:lignes` (source faisant foi).
- **Cohérence des types :** `getContenu(String)`, `mettreAJour(String,String)`, `reinitialiser(String)`, `construitLibelle(Annonce)`, `listerModeles()` employés de façon identique entre tasks ; `ContenuService` nouveau constructeur `(HtmlConverterService, ModeleEmailService)` répercuté dans le test (Task 7).
