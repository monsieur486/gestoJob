package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.StatutAnnonce;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.AnnonceRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

// Une méthode de test par comportement du service : scinder la classe en plusieurs fichiers
// éparpillerait les mêmes mocks et le même contexte sans rien clarifier.
@SuppressWarnings("PMD.TooManyMethods")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnnonceCreationServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;
    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private ContactService contactService;
    @Mock
    private UtilisateurCourant utilisateurCourant;

    private AnnonceCreationService creationService;

    @BeforeEach
    void setUp() {
        creationService = new AnnonceCreationService(annonceRepository, utilisateurCourant,
                entrepriseService, contactService);
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
        when(entrepriseService.existe(any())).thenReturn(true);
    }

    @Test
    void saveForm_poseLeTitulaireCourant() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(10).contactId(null).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        creationService.saveForm(form);

        ArgumentCaptor<Annonce> captor = ArgumentCaptor.forClass(Annonce.class);
        verify(annonceRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulaire()).isEqualTo(Titulaire.RENEE);
    }

    @Test
    void saveForm_contactAutreTitulaire_refuse() {
        when(contactService.getContact(3L)).thenThrow(new NoSuchElementException());
        AnnonceForm form = AnnonceForm.builder().entrepriseId(10).contactId(3L).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        assertThatThrownBy(() -> creationService.saveForm(form)).isInstanceOf(NoSuchElementException.class);

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void saveForm_entrepriseAutreTitulaire_refuse() {
        when(entrepriseService.existe(99)).thenReturn(false);
        AnnonceForm form = AnnonceForm.builder().entrepriseId(99).contactId(null).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        assertThatThrownBy(() -> creationService.saveForm(form)).isInstanceOf(NoSuchElementException.class);

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void saveForm_sansContact_creeAnnonceEnvoyeeAvecDate() {
        AnnonceForm form = AnnonceForm.builder()
                .entrepriseId(10).contactId(null).typeAnnonce(1).contenuId(2)
                .poste("Dev").reference("R1").build();

        creationService.saveForm(form);

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

        creationService.saveForm(form);

        ArgumentCaptor<Annonce> captor = ArgumentCaptor.forClass(Annonce.class);
        verify(annonceRepository).save(captor.capture());
        Annonce saved = captor.getValue();
        assertThat(saved.getStatusAnnonce()).isEqualTo(1);
        assertThat(saved.getContactId()).isEqualTo(3L);
        assertThat(saved.getDateEnvoi()).isNull();
    }

    @Test
    void creerAvecLettre_nettoieEtEnregistreLaLettre() {
        when(annonceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AnnonceForm form = AnnonceForm.builder().entrepriseId(3).contactId(8L).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        Annonce a = creationService.creerAvecLettre(form,
                "<p>{{POLITESSE}}</p><script>alert(1)</script><p>Acme</p>");

        assertThat(a.getLettrePersonnalisee()).contains("{{POLITESSE}}").contains("Acme").doesNotContain("script");
        assertThat(a.getStatusAnnonce()).isEqualTo(StatutAnnonce.BOITE_ENVOI.getCode());
        assertThat(a.getTitulaire()).isEqualTo(Titulaire.RENEE);
    }

    @Test
    void creerAvecLettre_sansPolitesse_refuse() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(3).contactId(8L).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        assertThatThrownBy(() -> creationService.creerAvecLettre(form, "<p>Bonjour</p>"))
                .isInstanceOf(MetierException.class).hasMessageContaining("{{POLITESSE}}");

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void creerAvecLettre_lettreNulle_refuse() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(3).build();

        assertThatThrownBy(() -> creationService.creerAvecLettre(form, null))
                .isInstanceOf(MetierException.class).hasMessageContaining("{{POLITESSE}}");

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void creerAvecLettre_politesseSupprimeeParLeNettoyage_refuse() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(3).contactId(8L).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        assertThatThrownBy(() -> creationService.creerAvecLettre(form,
                "<script>{{POLITESSE}}</script><p>x</p>"))
                .isInstanceOf(MetierException.class).hasMessageContaining("{{POLITESSE}}");

        verify(annonceRepository, never()).save(any());
    }

    @Test
    void creerAvecLettre_sansContact_estUneCandidatureSiteDejaEnvoyee() {
        when(annonceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AnnonceForm form = AnnonceForm.builder().entrepriseId(3).contactId(null).typeAnnonce(1)
                .contenuId(2).poste("Dev").reference("R1").build();

        Annonce a = creationService.creerAvecLettre(form, "<p>{{POLITESSE}}</p>");

        assertThat(a.getStatusAnnonce()).isEqualTo(StatutAnnonce.EN_COURS.getCode());
        assertThat(a.getDateEnvoi()).isNotNull();
    }

    @Test
    void saveForm_sansEntreprise_leveMetierException() {
        AnnonceForm form = AnnonceForm.builder().entrepriseId(null).build();

        assertThatThrownBy(() -> creationService.saveForm(form))
                .isInstanceOf(MetierException.class)
                .hasMessageContaining("Entreprise obligatoire");

        verify(annonceRepository, never()).save(any());
    }
}
