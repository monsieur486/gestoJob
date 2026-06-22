package com.mr486.gestojob.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

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
    void pageLogin_estAccessibleSansAuthentification() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void connexion_reussit_avecLesIdentifiantsParDefaut() throws Exception {
        mvc.perform(formLogin("/login").user("utilisateur").password("Mdp12345*"))
                .andExpect(authenticated());
    }

    @Test
    void connexion_echoue_avecUnMauvaisMotDePasse() throws Exception {
        mvc.perform(formLogin("/login").user("utilisateur").password("faux"))
                .andExpect(unauthenticated());
    }
}
