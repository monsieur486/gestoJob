package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.dto.RechercheAnnonceForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

// Une méthode de test par comportement du service : scinder la classe en plusieurs fichiers
// éparpillerait les mêmes mocks et le même contexte sans rien clarifier.
@SuppressWarnings("PMD.TooManyMethods")
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
    private LibelleService libelleService;
    @Mock
    private UtilisateurCourant utilisateurCourant;

    private AnnonceService annonceService;

    @BeforeEach
    void setUp() {
        // Le mapper (chargement des entreprises/contacts) est utilisé en vrai pour
        // que les tests de construction d'AnnonceListe restent significatifs ;
        // ses dépendances restent mockées.
        AnnonceListeMapper annonceListeMapper =
                new AnnonceListeMapper(entrepriseService, contactService, libelleService);
        annonceService = new AnnonceService(annonceRepository, annonceListeMapper,
                utilisateurCourant, entrepriseService, contactService);
        ReflectionTestUtils.setField(annonceService, "maxAnnoncesParPage", 10);
        ReflectionTestUtils.setField(annonceService, "maxPositifsParPage", 10);
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
        when(entrepriseService.existe(any())).thenReturn(true);
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
        when(annonceRepository.findAllByTitulaireAndEntrepriseIdOrderByStatusAnnonceAsc(Titulaire.RENEE, 10))
                .thenReturn(new java.util.ArrayList<>(List.of(janvier, sansDate, juin)));

        List<AnnonceListe> result = annonceService.annoncesListeByEntrepriseId(10);

        // juin (plus récent) -> janvier -> sans date (en dernier)
        assertThat(result).extracting(AnnonceListe::getId).containsExactly(2L, 1L, 3L);
    }

    @Test
    void deleteAnnonce_supprimeViaLeRepository() {
        Annonce a = Annonce.builder().id(7L).build();
        when(annonceRepository.findByIdAndTitulaire(7L, Titulaire.RENEE)).thenReturn(Optional.of(a));

        annonceService.deleteAnnonce(7L);

        verify(annonceRepository).delete(a);
    }

    @Test
    void deleteAnnonce_idAutreTitulaire_neSupprimeRien() {
        when(annonceRepository.findByIdAndTitulaire(8L, Titulaire.RENEE)).thenReturn(Optional.empty());

        annonceService.deleteAnnonce(8L);

        verify(annonceRepository, never()).delete(any());
        verify(annonceRepository, never()).deleteById(any());
    }

    @Test
    void getAnnonce_idAutreTitulaire_seComporteCommeInconnu() {
        when(annonceRepository.findByIdAndTitulaire(8L, Titulaire.RENEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> annonceService.getAnnonce(8L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getAnnonce_idDuTitulaire_laRetourne() {
        Annonce a = Annonce.builder().id(7L).build();
        when(annonceRepository.findByIdAndTitulaire(7L, Titulaire.RENEE)).thenReturn(Optional.of(a));

        assertThat(annonceService.getAnnonce(7L)).isSameAs(a);
    }

    @Test
    void countAnnonces_compteLeTitulaireCourant() {
        when(annonceRepository.countByTitulaire(Titulaire.RENEE)).thenReturn(12L);

        assertThat(annonceService.countAnnonces()).isEqualTo(12L);
    }

    @Test
    void saveForm_poseLeTitulaireCourant() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(10).contactId(null).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        annonceService.saveForm(form);

        ArgumentCaptor<Annonce> captor = ArgumentCaptor.forClass(Annonce.class);
        verify(annonceRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulaire()).isEqualTo(Titulaire.RENEE);
    }

    @Test
    void saveForm_contactAutreTitulaire_refuse() {
        when(contactService.getContact(3L)).thenThrow(new NoSuchElementException());
        AnnonceForm form = AnnonceForm.builder().entrepriseId(10).contactId(3L).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        assertThatThrownBy(() -> annonceService.saveForm(form)).isInstanceOf(NoSuchElementException.class);

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void saveForm_entrepriseAutreTitulaire_refuse() {
        when(entrepriseService.existe(99)).thenReturn(false);
        AnnonceForm form = AnnonceForm.builder().entrepriseId(99).contactId(null).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        assertThatThrownBy(() -> annonceService.saveForm(form)).isInstanceOf(NoSuchElementException.class);

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void saveForm_sansContact_creeAnnonceEnvoyeeAvecDate() {
        AnnonceForm form = AnnonceForm.builder()
                .entrepriseId(10).contactId(null).typeAnnonce(1).contenuId(2)
                .poste("Dev").reference("R1").build();

        annonceService.saveForm(form);

        ArgumentCaptor<Annonce> captor = ArgumentCaptor.forClass(Annonce.class);
        verify(annonceRepository).save(captor.capture());
        Annonce saved = captor.getValue();
        assertThat(saved.getStatusAnnonce()).isEqualTo(2);
        assertThat(saved.getContactId()).isNull();
        assertThat(saved.getDateEnvoi()).isNotNull();
    }

    @Test
    void saveForm_avecContact_creeAnnonceEnAttenteSansDate() {
        AnnonceForm form = AnnonceForm.builder()
                .entrepriseId(10).contactId(3L).typeAnnonce(1).contenuId(2)
                .poste("Dev").reference("R1").build();

        annonceService.saveForm(form);

        ArgumentCaptor<Annonce> captor = ArgumentCaptor.forClass(Annonce.class);
        verify(annonceRepository).save(captor.capture());
        Annonce saved = captor.getValue();
        assertThat(saved.getStatusAnnonce()).isEqualTo(1);
        assertThat(saved.getContactId()).isEqualTo(3L);
        assertThat(saved.getDateEnvoi()).isNull();
    }

    @Test
    void saveForm_sansEntreprise_leveMetierException() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(null).build();

        assertThatThrownBy(() -> annonceService.saveForm(form))
                .isInstanceOf(MetierException.class)
                .hasMessageContaining("Entreprise obligatoire");

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void searchAnnoncesPage_texteVideSansArchives_chercheToutSaufArchive() {
        when(annonceRepository.findAllByStatusAnnonceNotOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(6), any(Pageable.class)))
                .thenReturn(Page.empty());

        RechercheAnnonceForm form = RechercheAnnonceForm.builder().recherche("").avecArchives(false).build();
        annonceService.searchAnnoncesPage(form, 0);

        verify(annonceRepository).findAllByStatusAnnonceNotOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(6), any(Pageable.class));
    }

    @Test
    void searchAnnoncesPage_texteVideAvecArchives_chercheToutesLesAnnonces() {
        when(annonceRepository.findAllOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), any(Pageable.class))).thenReturn(Page.empty());

        RechercheAnnonceForm form = RechercheAnnonceForm.builder().recherche("  ").avecArchives(true).build();
        annonceService.searchAnnoncesPage(form, 0);

        verify(annonceRepository).findAllOrderByDateEnvoiDesc(eq(Titulaire.RENEE), any(Pageable.class));
    }

    @Test
    void searchAnnoncesPage_filtreParTitulaire() {
        when(annonceRepository.search(
                eq(Titulaire.RENEE), eq("java"), eq(false), any(Pageable.class))).thenReturn(Page.empty());

        RechercheAnnonceForm form = RechercheAnnonceForm.builder().recherche("java").avecArchives(false).build();
        annonceService.searchAnnoncesPage(form, 0);

        verify(annonceRepository).search(eq(Titulaire.RENEE), eq("java"), eq(false), any(Pageable.class));
    }

    @Test
    void getAllPositifListePage_filtreParTitulaire() {
        when(annonceRepository.findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(5), any(Pageable.class)))
                .thenReturn(Page.empty());

        annonceService.getAllPositifListePage(0);

        verify(annonceRepository).findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(5), any(Pageable.class));
    }

    @Test
    void annoncesEnAttenteEnvoiEmailPage_construitLesInfos_avecEntrepriseEtContact() {
        Annonce a = Annonce.builder().id(1L).entrepriseId(10).contactId(5L)
                .typeAnnonce(1).typeContenu(1).poste("Dev").reference("R").statusAnnonce(1).build();
        when(annonceRepository.findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(1), any(Pageable.class)))
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
        when(annonceRepository.findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(1), any(Pageable.class)))
                .thenReturn(Page.empty());

        annonceService.annoncesEnAttenteEnvoiEmailPage(0);

        verify(annonceRepository).findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(1), any(Pageable.class));
    }

    @Test
    void annoncesEnAttenteEnvoiEmailPage_infosSite_siPasDeContact() {
        Annonce a = Annonce.builder().id(2L).entrepriseId(10).contactId(null)
                .typeAnnonce(0).typeContenu(0).statusAnnonce(2).build();
        when(annonceRepository.findAllByTitulaireAndStatusAnnonceOrderByDateEnvoiDesc(
                eq(Titulaire.RENEE), eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));
        when(entrepriseService.getEntreprisesByIds(any()))
                .thenReturn(Map.of(10, Entreprise.builder().id(10).nom("ACME").build()));

        Page<AnnonceListe> result = annonceService.annoncesEnAttenteEnvoiEmailPage(0);

        assertThat(result.getContent().get(0).getInfo()).contains("site").contains("G");
        assertThat(result.getContent().get(0).getType()).contains("S");
    }
}
