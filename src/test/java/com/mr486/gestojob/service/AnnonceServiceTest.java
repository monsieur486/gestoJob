package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.dto.RechercheAnnonceForm;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.persistance.AnnonceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnonceServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;
    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private ContactService contactService;
    @Mock
    private ContenuService contenuService;

    private AnnonceService annonceService;

    @BeforeEach
    void setUp() {
        // Le mapper (chargement des entreprises/contacts) est utilisé en vrai pour
        // que les tests de construction d'AnnonceListe restent significatifs ;
        // ses dépendances restent mockées.
        AnnonceListeMapper annonceListeMapper = new AnnonceListeMapper(entrepriseService, contactService);
        annonceService = new AnnonceService(annonceRepository, contactService, contenuService, annonceListeMapper);
        ReflectionTestUtils.setField(annonceService, "maxAnnoncesParPage", 10);
        ReflectionTestUtils.setField(annonceService, "maxPositifsParPage", 10);
        when(entrepriseService.getEntreprisesByIds(any())).thenReturn(Collections.emptyMap());
        when(contactService.getContactsByIds(any())).thenReturn(Collections.emptyMap());
    }

    @Test
    void annoncesListeByEntrepriseId_triParDateDecroissante_nullsEnDernier() {
        Annonce janvier = Annonce.builder()
                .id(1L).entrepriseId(10).typeAnnonce(0).typeContenu(0).statusAnnonce(2)
                .dateEnvoi(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)).build();
        Annonce juin = Annonce.builder()
                .id(2L).entrepriseId(10).typeAnnonce(0).typeContenu(0).statusAnnonce(2)
                .dateEnvoi(OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC)).build();
        Annonce sansDate = Annonce.builder()
                .id(3L).entrepriseId(10).typeAnnonce(0).typeContenu(0).statusAnnonce(1)
                .dateEnvoi(null).build();

        // volontairement non triées en entrée
        when(annonceRepository.findAllByEntrepriseIdOrderByStatusAnnonceAsc(10))
                .thenReturn(new java.util.ArrayList<>(List.of(janvier, sansDate, juin)));

        List<AnnonceListe> result = annonceService.annoncesListeByEntrepriseId(10);

        // juin (plus récent) -> janvier -> sans date (en dernier)
        assertThat(result).extracting(AnnonceListe::getId).containsExactly(2L, 1L, 3L);
    }

    @Test
    void getMessageDePolitesse_avecContact_renvoieLaFormuleDuContact() {
        Annonce a = Annonce.builder().id(1L).contactId(5L).statusAnnonce(1).build();
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(contactService.getContact(5L)).thenReturn(
                Contact.builder().id(5L).entrepriseId(10).email("a@b.fr")
                        .formuleDePolistesse(1).contact("Durand").build());

        assertThat(annonceService.getMessageDePolitesse(1L)).isEqualTo("Monsieur Durand,");
    }

    @Test
    void getMessageDePolitesse_sansContact_renvoieFormuleGenerique() {
        Annonce a = Annonce.builder().id(1L).contactId(null).statusAnnonce(2).build();
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThat(annonceService.getMessageDePolitesse(1L)).isEqualTo("Madame, Monsieur,");
    }

    @Test
    void setDepasse_metLeStatutA3() {
        annonceService.setDepasse(7L);
        verify(annonceRepository).updateStatusAnnonce(7L, 3);
    }

    @Test
    void setRefus_metLeStatutA4() {
        annonceService.setRefus(7L);
        verify(annonceRepository).updateStatusAnnonce(7L, 4);
    }

    @Test
    void setAccepte_metLeStatutA5() {
        annonceService.setAccepte(7L);
        verify(annonceRepository).updateStatusAnnonce(7L, 5);
    }

    @Test
    void getAnnonceTxtContenuById_concateneLibelleEtContenuTexte() {
        Annonce a = Annonce.builder().id(1L).contactId(null).typeAnnonce(1)
                .reference("R1").poste("Dev").typeContenu(0).statusAnnonce(2).build();
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(contenuService.getTextContenu(eq("Dev"), eq(0), any())).thenReturn("CORPS_TXT");

        String result = annonceService.getAnnonceTxtContenuById(1L);

        assertThat(result).contains("CORPS_TXT");
        assertThat(result).contains("R1");
    }

    @Test
    void searchAnnoncesPage_texteVideSansArchives_chercheLesEnvoyees() {
        when(annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(2), any(Pageable.class)))
                .thenReturn(Page.empty());

        RechercheAnnonceForm form = RechercheAnnonceForm.builder().recherche("").avecArchives(false).build();
        annonceService.searchAnnoncesPage(form, 0);

        verify(annonceRepository).findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(2), any(Pageable.class));
    }

    @Test
    void searchAnnoncesPage_texteVideAvecArchives_chercheToutesLesAnnonces() {
        when(annonceRepository.findAllOrderByDateEnvoiDesc(any(Pageable.class))).thenReturn(Page.empty());

        RechercheAnnonceForm form = RechercheAnnonceForm.builder().recherche("  ").avecArchives(true).build();
        annonceService.searchAnnoncesPage(form, 0);

        verify(annonceRepository).findAllOrderByDateEnvoiDesc(any(Pageable.class));
    }

    @Test
    void searchAnnoncesPage_avecTexte_appelleLaRechercheMultiChamps() {
        when(annonceRepository.search(eq("java"), eq(false), any(Pageable.class))).thenReturn(Page.empty());

        RechercheAnnonceForm form = RechercheAnnonceForm.builder().recherche("java").avecArchives(false).build();
        annonceService.searchAnnoncesPage(form, 0);

        verify(annonceRepository).search(eq("java"), eq(false), any(Pageable.class));
    }

    @Test
    void getAllPositifListePage_chercheLesAnnoncesPositives() {
        when(annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(5), any(Pageable.class)))
                .thenReturn(Page.empty());

        annonceService.getAllPositifListePage(0);

        verify(annonceRepository).findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(5), any(Pageable.class));
    }

    @Test
    void annoncesEnAttenteEnvoiEmailPage_construitLesInfos_avecEntrepriseEtContact() {
        Annonce a = Annonce.builder().id(1L).entrepriseId(10).contactId(5L)
                .typeAnnonce(1).typeContenu(1).poste("Dev").reference("R").statusAnnonce(1).build();
        when(annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));
        when(entrepriseService.getEntreprisesByIds(any()))
                .thenReturn(Map.of(10, Entreprise.builder().id(10).nom("ACME").build()));
        when(contactService.getContactsByIds(any()))
                .thenReturn(Map.of(5L, Contact.builder().id(5L).entrepriseId(10).email("c@acme.fr").build()));

        Page<AnnonceListe> result = annonceService.annoncesEnAttenteEnvoiEmailPage(0);

        assertThat(result.getContent()).hasSize(1);
        AnnonceListe liste = result.getContent().get(0);
        assertThat(liste.getInfo()).contains("ACME").contains("c@acme.fr").contains("MS");
        assertThat(liste.getType()).contains("A");
    }

    @Test
    void annoncesEnAttenteEnvoiEmailPage_chercheLeStatut1Pagine() {
        when(annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(1), any(Pageable.class)))
                .thenReturn(Page.empty());

        annonceService.annoncesEnAttenteEnvoiEmailPage(0);

        verify(annonceRepository).findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(1), any(Pageable.class));
    }

    @Test
    void annoncesEnAttenteEnvoiEmailPage_infosSite_siPasDeContact() {
        Annonce a = Annonce.builder().id(2L).entrepriseId(10).contactId(null)
                .typeAnnonce(0).typeContenu(0).statusAnnonce(2).build();
        when(annonceRepository.findAllByStatusAnnonceOrderByDateEnvoiDesc(eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));
        when(entrepriseService.getEntreprisesByIds(any()))
                .thenReturn(Map.of(10, Entreprise.builder().id(10).nom("ACME").build()));

        Page<AnnonceListe> result = annonceService.annoncesEnAttenteEnvoiEmailPage(0);

        assertThat(result.getContent().get(0).getInfo()).contains("site").contains("G");
        assertThat(result.getContent().get(0).getType()).contains("S");
    }
}
