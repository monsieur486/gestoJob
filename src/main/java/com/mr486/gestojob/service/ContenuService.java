package com.mr486.gestojob.service;

import com.mr486.gestojob.configuration.ApplicationConfiguration;
import com.mr486.gestojob.model.TypeContenu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Service de génération des contenus des candidatures (lettres de motivation).
 * Produit, selon le type de poste, les versions HTML et texte du message en
 * substituant la formule de politesse et l'intitulé du poste dans des modèles.
 */
@Service
@RequiredArgsConstructor
public class ContenuService {

    private final HtmlConverterService htmlConverterService;

    // Modèle HTML pour une candidature de développeur Java généraliste.
    private static String annoncePosteGeneralHtmlTemplate() {
        return """
                <p>{{POLITESSE}}</p>
                <p>Passionn&eacute; par l&rsquo;informatique et les nouvelles technologies depuis de nombreuses ann&eacute;es, j&rsquo;ai r&eacute;cemment consolid&eacute; mes comp&eacute;tences en d&eacute;veloppement Java &agrave; travers une formation dipl&ocirc;mante de&nbsp;<strong>D&eacute;veloppeur d&rsquo;applications Java</strong> (niveau 6) r&eacute;alis&eacute;e chez OpenClassrooms. Je souhaite aujourd&rsquo;hui mettre mes comp&eacute;tences techniques et mon engagement au service de votre &eacute;quipe au poste <strong>{{POSTE}}</strong>.</p>
                <p>Au cours de ma formation et de mes projets personnels, j&rsquo;ai acquis une solide ma&icirc;trise de&nbsp;<strong>Java</strong>, de l&rsquo;&eacute;cosyst&egrave;me&nbsp;<strong>Spring Boot</strong>, ainsi que des bases essentielles du d&eacute;veloppement backend moderne : conception d&rsquo;API REST, acc&egrave;s aux bases de donn&eacute;es (SQL et NoSQL), tests unitaires, int&eacute;gration continue et bonnes pratiques de conception (SOLID, TDD). J&rsquo;ai &eacute;galement travaill&eacute; avec des outils et environnements courants tels que&nbsp;<strong>Docker</strong>,&nbsp;<strong>Git</strong>,&nbsp;<strong>Linux</strong>, et d&eacute;couvert les architectures&nbsp;<strong>microservices</strong>.</p>
                <p>Mon parcours professionnel ant&eacute;rieur, notamment en tant que r&eacute;f&eacute;rent technique et responsable informatique, m&rsquo;a permis de d&eacute;velopper des qualit&eacute;s essentielles comme la rigueur, l&rsquo;autonomie, le sens du service et la capacit&eacute; &agrave; travailler en &eacute;quipe. Habitu&eacute; &agrave; &eacute;voluer dans des environnements exigeants, je sais m&rsquo;adapter rapidement et rester fiable face aux enjeux techniques et organisationnels.</p>
                <p>Motiv&eacute;, curieux et investi, je souhaite rejoindre une structure o&ugrave; je pourrai continuer &agrave; progresser techniquement, contribuer activement aux projets et relever de nouveaux d&eacute;fis en d&eacute;veloppement Java.</p>
                <p>Je serais ravi de pouvoir &eacute;changer avec vous lors d&rsquo;un entretien afin de vous pr&eacute;senter plus en d&eacute;tail mon profil et ma motivation.</p>
                <p>Je vous prie d&rsquo;agr&eacute;er, {{POLITESSE}} l&rsquo;expression de ma parfaite considération.</p>
                <p>Laurent Touret</p>
                        <p><a href="https://mr486.com/assets/files/CV-Laurent-Touret.pdf" target="_blank" rel="noopener noreferrer">🎓 CV Lien cliquable</a></p>
                        <p><a href="https://mr486.com" target="_blank" rel="noopener noreferrer">🌐 Site de présentation https://mr486.com</a></p>
                """;
    }

    // Modèle texte pour une candidature de développeur Java généraliste.
    private static String annoncePosteGeneralTxtTemplate() {
        return """
                {{POLITESSE}}
                
                Passionné par l’informatique et les nouvelles technologies depuis de nombreuses années, j’ai récemment consolidé mes compétences en développement Java à travers une formation diplômante de Développeur d’applications Java (niveau 6) réalisée chez OpenClassrooms. Je souhaite aujourd’hui mettre mes compétences techniques et mon engagement au service de votre équipe au poste {{POSTE}}.
                
                Au cours de ma formation et de mes projets personnels, j’ai acquis une solide maîtrise de Java, de l’écosystème Spring Boot, ainsi que des bases essentielles du développement backend moderne : conception d’API REST, accès aux bases de données (SQL et NoSQL), tests unitaires, intégration continue et bonnes pratiques de conception (SOLID, TDD). J’ai également travaillé avec des outils et environnements courants tels que Docker, Git, Linux, et découvert les architectures microservices.
                
                Mon parcours professionnel antérieur, notamment en tant que référent technique et responsable informatique, m’a permis de développer des qualités essentielles comme la rigueur, l’autonomie, le sens du service et la capacité à travailler en équipe. Habitué à évoluer dans des environnements exigeants, je sais m’adapter rapidement et rester fiable face aux enjeux techniques et organisationnels.
                
                Motivé, curieux et investi, je souhaite rejoindre une structure où je pourrai continuer à progresser techniquement, contribuer activement aux projets et relever de nouveaux défis en développement Java.
                
                Je serais ravi de pouvoir échanger avec vous lors d’un entretien afin de vous présenter plus en détail mon profil et ma motivation.
                
                Je vous prie d’agréer, {{POLITESSE}} l’expression de ma parfaite considération.
                
                
                Laurent Touret
                
                CV https://www.mr486.com/assets/files/CV-Laurent-Touret.pdf
                Site de présentation https://mr486.com
                """;
    }

    // Modèle HTML pour une candidature de développeur Java orienté microservices.
    private static String annoncePosteMicroserviceHtmlTemplate() {
        return """
                <p>{{POLITESSE}}</p>
                <p>D&eacute;veloppeur Java passionn&eacute; par les architectures distribu&eacute;es et les technologies backend modernes, je souhaite vous proposer ma candidature pour un poste <strong>{{POSTE}}</strong> au sein de votre &eacute;quipe.</p>
                <p>Dipl&ocirc;m&eacute; <strong>D&eacute;veloppeur d&rsquo;applications Java (niveau 6 &ndash; OpenClassrooms)</strong>, j&rsquo;ai acquis une solide exp&eacute;rience sur l&rsquo;&eacute;cosyst&egrave;me <strong>Spring Boot</strong> et plus particuli&egrave;rement sur les <strong>architectures microservices</strong>. Dans mes projets r&eacute;cents, j&rsquo;ai con&ccedil;u et mis en &oelig;uvre des applications distribu&eacute;es reposant sur <strong>Spring Cloud</strong>, avec <strong>Eureka</strong> pour la d&eacute;couverte de services et une <strong>API Gateway</strong> pour la centralisation des flux, la s&eacute;curit&eacute; et le routage des requ&ecirc;tes.</p>
                <p>J&rsquo;ai travaill&eacute; sur la cr&eacute;ation de <strong>microservices REST ind&eacute;pendants</strong>, communiquant entre eux via <strong>OpenFeign</strong> et des API REST, int&eacute;grant la gestion des erreurs, la r&eacute;silience et les bonnes pratiques de d&eacute;couplage. J&rsquo;ai &eacute;galement abord&eacute; les probl&eacute;matiques de performance et d&rsquo;asynchronisme gr&acirc;ce &agrave; <strong>Spring WebFlux</strong>, ainsi que la mise en cache et la gestion des donn&eacute;es via <strong>PostgreSQL</strong>, <strong>MongoDB</strong> et <strong>Redis</strong>.</p>
                <p>L&rsquo;ensemble de ces projets a &eacute;t&eacute; conteneuris&eacute; avec <strong>Docker</strong> et orchestr&eacute; via <strong>Docker Compose</strong>, me permettant de simuler des environnements proches de la production, avec mont&eacute;e en charge, profils d&rsquo;ex&eacute;cution et variables d&rsquo;environnement. J&rsquo;accorde une importance particuli&egrave;re &agrave; la qualit&eacute; du code, aux <strong>tests unitaires</strong>, &agrave; l&rsquo;application des principes <strong>SOLID</strong>, ainsi qu&rsquo;&agrave; l&rsquo;int&eacute;gration continue.</p>
                <p>Mon parcours professionnel ant&eacute;rieur dans des fonctions techniques et de support m&rsquo;a apport&eacute; rigueur, sens des responsabilit&eacute;s et une forte capacit&eacute; d&rsquo;adaptation. Aujourd&rsquo;hui, je souhaite m&rsquo;investir pleinement dans une &eacute;quipe technique afin de continuer &agrave; monter en comp&eacute;tences sur les architectures microservices et contribuer activement &agrave; des projets Java &agrave; forte valeur ajout&eacute;e.</p>
                <p>Je serais heureux de pouvoir &eacute;changer avec vous afin de vous pr&eacute;senter plus en d&eacute;tail mon parcours, mes projets et ma motivation.</p>
                <p>Je vous prie d&rsquo;agr&eacute;er, {{POLITESSE}} l&rsquo;expression de ma parfaite considération.</p>
                <p>Laurent Touret</p>
                        <p><a href="https://mr486.com/assets/files/CV-Laurent-Touret.pdf" target="_blank" rel="noopener noreferrer">🎓 CV Lien cliquable</a></p>
                        <p><a href="https://mr486.com" target="_blank" rel="noopener noreferrer">🌐 Site de présentation https://mr486.com</a></p>
                """;
    }

    // Modèle texte pour une candidature de développeur Java orienté microservices.
    private static String annoncePosteMicroserviceTxtTemplate() {
        return """
                {{POLITESSE}}
                
                Développeur Java passionné par les architectures distribuées et les technologies backend modernes, je souhaite vous proposer ma candidature pour un poste {{POSTE}} au sein de votre équipe.
                
                Diplômé Développeur d’applications Java (niveau 6 – OpenClassrooms), j’ai acquis une solide expérience sur l’écosystème Spring Boot et plus particulièrement sur les architectures microservices. Dans mes projets récents, j’ai conçu et mis en œuvre des applications distribuées reposant sur Spring Cloud, avec Eureka pour la découverte de services et une API Gateway pour la centralisation des flux, la sécurité et le routage des requêtes.
                
                J’ai travaillé sur la création de microservices REST indépendants, communiquant entre eux via OpenFeign et des API REST, intégrant la gestion des erreurs, la résilience et les bonnes pratiques de découplage. J’ai également abordé les problématiques de performance et d’asynchronisme grâce à Spring WebFlux, ainsi que la mise en cache et la gestion des données via PostgreSQL, MongoDB et Redis.
                
                L’ensemble de ces projets a été conteneurisé avec Docker et orchestré via Docker Compose, me permettant de simuler des environnements proches de la production, avec montée en charge, profils d’exécution et variables d’environnement. J’accorde une importance particulière à la qualité du code, aux tests unitaires, à l’application des principes SOLID, ainsi qu’à l’intégration continue.
                
                Mon parcours professionnel antérieur dans des fonctions techniques et de support m’a apporté rigueur, sens des responsabilités et une forte capacité d’adaptation. Aujourd’hui, je souhaite m’investir pleinement dans une équipe technique afin de continuer à monter en compétences sur les architectures microservices et contribuer activement à des projets Java à forte valeur ajoutée.
                
                Je serais heureux de pouvoir échanger avec vous afin de vous présenter plus en détail mon parcours, mes projets et ma motivation.
                
                Je vous prie d’agréer, {{POLITESSE}} l’expression de ma parfaite considération.
                
                
                Laurent Touret
                
                CV https://www.mr486.com/assets/files/CV-Laurent-Touret.pdf
                Site de présentation https://mr486.com
                """;
    }

    // Modèle HTML pour une candidature « nouvelle génération » axée IA agentique.
    private static String annoncePosteIaAgentiqueHtmlTemplate() {
        return """
                <p>{{POLITESSE}}</p>
                <p>Développeur Java back-end qui place l'<strong>IA agentique</strong> au cœur de sa méthode, je vous propose ma candidature au poste <strong>{{POSTE}}</strong>. Ma conviction tient en une phrase : <em>je développe autant avec mon expertise qu'avec une équipe d'agents IA spécialisés</em>, pour livrer plus vite, élargir mon périmètre et tenir une exigence de qualité constante.</p>
                <p>Côté socle technique, je m'appuie sur une formation diplômante de <strong>Développeur d'applications Java (niveau 6 – OpenClassrooms)</strong> et une solide maîtrise de <strong>Java 17/21</strong>, <strong>Spring Boot 3</strong>, <strong>Hibernate/JPA</strong>, des <strong>architectures microservices</strong> (Spring Cloud, REST/OpenAPI), avec une attention constante aux bonnes pratiques : <strong>SOLID</strong>, <strong>TDD</strong>, tests JUnit 5 / Mockito, intégration continue (GitHub Actions) et conteneurisation Docker.</p>
                <p>Ce qui distingue ma façon de travailler, c'est l'<strong>orchestration d'agents IA</strong> au quotidien. Avec des outils comme <strong>Claude Code</strong> et une pratique rigoureuse du <strong>prompt engineering</strong>, je délègue à des agents spécialisés l'exploration de code, la génération de tests, la revue et la refactorisation, tout en gardant la maîtrise de l'architecture et l'entière responsabilité des choix. Cette approche prolonge naturellement mon expertise du back-end vers le <strong>full-stack</strong>, sans rien céder sur la qualité ni la sécurité.</p>
                <p>Cette méthode, je l'ai éprouvée sur des projets concrets menés de bout en bout : <strong>GestoMS</strong>, un générateur de plateforme microservices complète (Keycloak, OAuth2), et <strong>Tarot Des Amis</strong>, une application temps réel en production (WebSocket, interface mobile-first) — du code aux tests jusqu'au déploiement.</p>
                <p>Vous rejoindre, c'est mettre cette productivité augmentée au service de vos projets : des livraisons accélérées, un périmètre élargi et une qualité de code outillée et mesurable. Une manière de développer résolument tournée vers demain, que je serais heureux d'apporter à votre équipe.</p>
                <p>Je serais ravi d'échanger avec vous lors d'un entretien afin de vous présenter ma démarche, mes projets et ma motivation.</p>
                <p>Je vous prie d'agréer, {{POLITESSE}} l'expression de ma parfaite considération.</p>
                <p>Laurent Touret</p>
                        <p><a href="https://mr486.com/assets/files/CV-Laurent-Touret.pdf" target="_blank" rel="noopener noreferrer">🎓 CV Lien cliquable</a></p>
                        <p><a href="https://mr486.com" target="_blank" rel="noopener noreferrer">🌐 Site de présentation https://mr486.com</a></p>
                """;
    }

    // Modèle texte pour une candidature « nouvelle génération » axée IA agentique.
    private static String annoncePosteIaAgentiqueTxtTemplate() {
        return """
                {{POLITESSE}}

                Développeur Java back-end qui place l'IA agentique au cœur de sa méthode, je vous propose ma candidature au poste {{POSTE}}. Ma conviction tient en une phrase : je développe autant avec mon expertise qu'avec une équipe d'agents IA spécialisés, pour livrer plus vite, élargir mon périmètre et tenir une exigence de qualité constante.

                Côté socle technique, je m'appuie sur une formation diplômante de Développeur d'applications Java (niveau 6 – OpenClassrooms) et une solide maîtrise de Java 17/21, Spring Boot 3, Hibernate/JPA, des architectures microservices (Spring Cloud, REST/OpenAPI), avec une attention constante aux bonnes pratiques : SOLID, TDD, tests JUnit 5 / Mockito, intégration continue (GitHub Actions) et conteneurisation Docker.

                Ce qui distingue ma façon de travailler, c'est l'orchestration d'agents IA au quotidien. Avec des outils comme Claude Code et une pratique rigoureuse du prompt engineering, je délègue à des agents spécialisés l'exploration de code, la génération de tests, la revue et la refactorisation, tout en gardant la maîtrise de l'architecture et l'entière responsabilité des choix. Cette approche prolonge naturellement mon expertise du back-end vers le full-stack, sans rien céder sur la qualité ni la sécurité.

                Cette méthode, je l'ai éprouvée sur des projets concrets menés de bout en bout : GestoMS, un générateur de plateforme microservices complète (Keycloak, OAuth2), et Tarot Des Amis, une application temps réel en production (WebSocket, interface mobile-first) — du code aux tests jusqu'au déploiement.

                Vous rejoindre, c'est mettre cette productivité augmentée au service de vos projets : des livraisons accélérées, un périmètre élargi et une qualité de code outillée et mesurable. Une manière de développer résolument tournée vers demain, que je serais heureux d'apporter à votre équipe.

                Je serais ravi d'échanger avec vous lors d'un entretien afin de vous présenter ma démarche, mes projets et ma motivation.

                Je vous prie d'agréer, {{POLITESSE}} l'expression de ma parfaite considération.


                Laurent Touret

                CV https://www.mr486.com/assets/files/CV-Laurent-Touret.pdf
                Site de présentation https://mr486.com
                """;
    }

    // Sélectionne le modèle selon le type de contenu (1 = microservices,
    // 2 = IA agentique, sinon généraliste) et la sortie souhaitée (HTML ou texte),
    // applique un poste par défaut si absent, puis substitue la politesse et le
    // poste dans le modèle. Type et politesse null sont ramenés à des valeurs sûres
    // (généraliste / salutation générique) pour éviter toute NPE.
    // En HTML, les saisies utilisateur sont échappées pour éviter l'injection.
    private String getContent(String poste, Integer typeContenu, String messageDePolitesse, Boolean isHtml) {
        int type = (typeContenu == null) ? TypeContenu.GENERAL.getCode() : typeContenu;
        String politesse = (messageDePolitesse == null)
                ? ApplicationConfiguration.SALUTATION_GENERIQUE
                : messageDePolitesse;
        String contenu = "";
        if (isHtml) {
            // Les saisies utilisateur (poste, politesse) sont échappées pour éviter
            // toute injection HTML dans le corps de l'email. Les valeurs par défaut
            // ci-dessous sont des littéraux déjà encodés (&eacute;, ...) et ne le sont pas.
            String safePolitesse = HtmlUtils.htmlEscape(politesse);
            if (type == TypeContenu.MICROSERVICES.getCode()) {
                String safePoste = (poste == null || poste.isEmpty())
                        ? "de d&eacute;veloppeur Java orient&eacute; microservices"
                        : HtmlUtils.htmlEscape(poste);
                contenu = annoncePosteMicroserviceHtmlTemplate()
                        .replace("{{POLITESSE}}", safePolitesse)
                        .replace("{{POSTE}}", safePoste);
            } else if (type == TypeContenu.IA.getCode()) {
                String safePoste = (poste == null || poste.isEmpty())
                        ? "de d&eacute;veloppeur Java back-end orient&eacute; IA agentique"
                        : HtmlUtils.htmlEscape(poste);
                contenu = annoncePosteIaAgentiqueHtmlTemplate()
                        .replace("{{POLITESSE}}", safePolitesse)
                        .replace("{{POSTE}}", safePoste);
            } else {
                String safePoste = (poste == null || poste.isEmpty())
                        ? "de d&eacute;veloppeur Java"
                        : HtmlUtils.htmlEscape(poste);
                contenu = annoncePosteGeneralHtmlTemplate()
                        .replace("{{POLITESSE}}", safePolitesse)
                        .replace("{{POSTE}}", safePoste);
            }
        } else {
            if (type == TypeContenu.MICROSERVICES.getCode()) {
                if (poste == null || poste.isEmpty()) poste = "de développeur Java orienté microservices";
                contenu = annoncePosteMicroserviceTxtTemplate()
                        .replace("{{POLITESSE}}", politesse)
                        .replace("{{POSTE}}", poste);
            } else if (type == TypeContenu.IA.getCode()) {
                if (poste == null || poste.isEmpty()) poste = "de développeur Java back-end orienté IA agentique";
                contenu = annoncePosteIaAgentiqueTxtTemplate()
                        .replace("{{POLITESSE}}", politesse)
                        .replace("{{POSTE}}", poste);
            } else {
                if (poste == null || poste.isEmpty()) poste = "de développeur Java";
                contenu = annoncePosteGeneralTxtTemplate()
                        .replace("{{POLITESSE}}", politesse)
                        .replace("{{POSTE}}", poste);
            }

        }

        return contenu;
    }

    /**
     * Génère la version HTML du contenu de la candidature.
     *
     * <p><b>Exemple :</b> un poste contenant « &lt;script&gt; » est échappé en « &amp;lt;script&amp;gt; » dans le HTML produit.</p>
     *
     * @param poste              intitulé du poste visé (une valeur par défaut est utilisée si vide)
     * @param typeContenu        type de contenu (1 = microservices, 2 = IA agentique, null/autre = généraliste)
     * @param messageDePolitesse formule de politesse à insérer (la salutation générique est utilisée si null)
     * @return le contenu HTML de la candidature
     */
    public String getHtmlContenu(String poste, Integer typeContenu, String messageDePolitesse) {
        return getContent(poste, typeContenu, messageDePolitesse, true);
    }

    /**
     * Génère la version texte du contenu de la candidature.
     *
     * <p><b>Exemple :</b> avec typeContenu=1 et un poste vide, le texte produit utilise le poste par défaut « de développeur Java orienté microservices » ; le poste fourni n'est pas échappé (sortie texte brut).</p>
     *
     * @param poste              intitulé du poste visé (une valeur par défaut est utilisée si vide)
     * @param typeContenu        type de contenu (1 = microservices, 2 = IA agentique, null/autre = généraliste)
     * @param messageDePolitesse formule de politesse à insérer (la salutation générique est utilisée si null)
     * @return le contenu texte de la candidature
     */
    public String getTextContenu(String poste, Integer typeContenu, String messageDePolitesse) {
        return getContent(poste, typeContenu, messageDePolitesse, false);
    }
}
