package com.mr486.gestojob.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@TestPropertySource(properties = {
        "gestojob.comptes.laurent.username=laurent-test",
        "gestojob.comptes.laurent.password=mdp-laurent",
        "gestojob.comptes.renee.username=renee-test",
        "gestojob.comptes.renee.password=mdp-renee",
        "gestojob.comptes.ft01.username=ft01-test",
        "gestojob.comptes.ft01.password=mdp-ft01",
        "gestojob.comptes.ft02.username=ft02-test",
        "gestojob.comptes.ft02.password=mdp-ft02"
})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserDetailsService userDetailsService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void pageProtegee_redirigeVersLogin_siNonAuthentifie() throws Exception {
        mvc.perform(get("/entreprises"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void accueil_redirigeVersLogin_siNonAuthentifie() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void pageLogin_estAccessibleSansAuthentification() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({
            "laurent-test,mdp-laurent",
            "renee-test,mdp-renee",
            "ft01-test,mdp-ft01",
            "ft02-test,mdp-ft02"
    })
    void connexion_reussit_pourChacunDesQuatreComptes(String utilisateur, String motDePasse) throws Exception {
        mvc.perform(formLogin("/login").user(utilisateur).password(motDePasse))
                .andExpect(authenticated());
    }

    @Test
    void connexion_echoue_avecUnMauvaisMotDePasse() throws Exception {
        mvc.perform(formLogin("/login").user("laurent-test").password("faux"))
                .andExpect(unauthenticated());
    }

    @Test
    void lecteur_postRefuse_403() throws Exception {
        mvc.perform(post("/entreprises/ajout").with(user("ft01").roles("LECTEUR")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecteur_formulaireCreationRefuse_403() throws Exception {
        mvc.perform(get("/entreprises/ajout").with(user("ft01").roles("LECTEUR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/entreprises/1/contacts/ajout").with(user("ft01").roles("LECTEUR")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "laurent-test,ROLE_TITULAIRE,DONNEES_LAURENT",
            "renee-test,ROLE_TITULAIRE,DONNEES_RENEE",
            "ft01-test,ROLE_LECTEUR,DONNEES_LAURENT",
            "ft02-test,ROLE_LECTEUR,DONNEES_RENEE"
    })
    void compte_porteExactementLesAutoritesDeSonTitulaire(String login, String role, String donnees) {
        Set<String> autorites = userDetailsService.loadUserByUsername(login).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(autorites).containsExactlyInAnyOrder(role, donnees);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/file/postMail",
            "/file/postMail/1",
            "/parametres/profil",
            "/parametres/modeles/CONTENU_IA",
            "/parametres/modeles/CONTENU_IA/reinitialiser",
            "/annonces/1/positif",
            "/annonces/1/negatif",
            "/annonces/1/supprimer",
            "/entreprises/1/activate",
            "/entreprises/1/desactivate",
            "/entreprises/1/annonces/ajout",
            "/entreprises/1/annonces/1/supprimer",
            "/entreprises/1/contacts/ajout",
            "/entreprises/1/contacts/1/supprimer"
    })
    void lecteur_postDActionRefuse_403(String chemin) throws Exception {
        mvc.perform(post(chemin).with(user("ft01").roles("LECTEUR")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecteur_putEtDeleteRefuses_403() throws Exception {
        mvc.perform(put("/entreprises/1").with(user("ft01").roles("LECTEUR")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/annonces/1").with(user("ft01").roles("LECTEUR")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecteur_consultationAutorisee() throws Exception {
        // Le Postgres de dev est requis par ce test (contexte complet) : la page se rend.
        mvc.perform(get("/annonces")
                        .with(user("ft01").roles("LECTEUR").authorities(
                                new SimpleGrantedAuthority("ROLE_LECTEUR"),
                                new SimpleGrantedAuthority("DONNEES_LAURENT"))))
                .andExpect(status().isOk());
    }

    @Test
    void titulaire_postAutorise() throws Exception {
        // Formulaire vide : le contrôleur le refuse par validation et redirige, preuve que
        // la requête a franchi la sécurité (un lecteur recevrait 403).
        mvc.perform(post("/entreprises/1/contacts/ajout")
                        .with(user("laurent").authorities(
                                new SimpleGrantedAuthority("ROLE_TITULAIRE"),
                                new SimpleGrantedAuthority("DONNEES_LAURENT")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        int statut = mvc.perform(post("/entreprises/ajout").with(user("laurent").roles("TITULAIRE")).with(csrf()))
                .andReturn().getResponse().getStatus();
        assertNotEquals(403, statut);
    }

    @Test
    void lecteur_peutSeDeconnecter() throws Exception {
        mvc.perform(post("/logout").with(user("ft01").roles("LECTEUR")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
