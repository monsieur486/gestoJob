package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.AnnonceForm;
import com.mr486.gestojob.dto.ContactForm;
import com.mr486.gestojob.dto.EntrepriseForm;
import com.mr486.gestojob.dto.api.AnnonceDemande;
import com.mr486.gestojob.dto.api.CandidatureDemande;
import com.mr486.gestojob.dto.api.CandidatureReponse;
import com.mr486.gestojob.dto.api.ContactDemande;
import com.mr486.gestojob.dto.api.EntrepriseApi;
import com.mr486.gestojob.dto.api.EntrepriseDemande;
import com.mr486.gestojob.dto.api.ModeleLettreApi;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.ContactRepository;
import com.mr486.gestojob.persistance.EntrepriseRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Tests unitaires de {@link CandidatureApiService} (dépendances mockées, validateur réel).
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects"})
class CandidatureApiServiceTest {

    @Mock private EntrepriseService entrepriseService;
    @Mock private ContactService contactService;
    @Mock private AnnonceCreationService annonceCreationService;
    @Mock private ModeleEmailService modeleEmailService;
    @Mock private ProfilService profilService;
    @Mock private EntrepriseRepository entrepriseRepository;
    @Mock private ContactRepository contactRepository;
    @Mock private UtilisateurCourant utilisateurCourant;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private CandidatureApiService service;

    @BeforeEach
    void init() {
        service = new CandidatureApiService(entrepriseService, contactService, annonceCreationService,
                modeleEmailService, profilService, entrepriseRepository, contactRepository,
                utilisateurCourant, validator);
        lenient().when(utilisateurCourant.titulaire()).thenReturn(Titulaire.LAURENT);
    }

    private static Entreprise entreprise(int id, boolean active) {
        return Entreprise.builder().id(id).nom("Acme").ville("Paris").codePostal("75001")
                .estActive(active).titulaire(Titulaire.LAURENT).build();
    }

    private static Annonce annonce(long id, int statut) {
        Annonce a = new Annonce();
        a.setId(id);
        a.setStatusAnnonce(statut);
        return a;
    }

    private static CandidatureDemande demande(EntrepriseDemande e, ContactDemande c) {
        return new CandidatureDemande(e, c, new AnnonceDemande(1, "Dev", "R1"), "<p>{{POLITESSE}}</p>");
    }

    @Test
    void modeleLettre_renvoieLeModeleIaEtLePosteParDefaut() {
        Profil profil = new Profil();
        when(modeleEmailService.getContenu(Titulaire.LAURENT, "CONTENU_IA")).thenReturn("<p>{{POLITESSE}}</p>");
        when(profilService.getProfil(Titulaire.LAURENT)).thenReturn(profil);
        when(profilService.posteParDefaut(profil, CleModele.CONTENU_IA)).thenReturn("Architecte");

        ModeleLettreApi m = service.modeleLettre();

        assertThat(m.cle()).isEqualTo("CONTENU_IA");
        assertThat(m.html()).isEqualTo("<p>{{POLITESSE}}</p>");
        assertThat(m.posteParDefaut()).isEqualTo("Architecte");
    }

    @Test
    void recherche_nomBlanc_refusee() {
        assertThatThrownBy(() -> service.rechercherEntreprises("  "))
                .isInstanceOf(MetierException.class)
                .hasMessage("Le nom recherché est obligatoire.");
        assertThatThrownBy(() -> service.rechercherEntreprises(null))
                .isInstanceOf(MetierException.class);
        verifyNoInteractions(entrepriseRepository, contactRepository);
    }

    @Test
    void recherche_chargeLesContactsEnUneRequete() {
        Entreprise e1 = entreprise(1, true);
        Entreprise e2 = entreprise(2, false);
        when(entrepriseRepository.findAllByTitulaireAndNomContainingIgnoreCaseOrderByNomAsc(
                eq(Titulaire.LAURENT), eq("ac"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1, e2)));
        Contact c = Contact.builder().id(7L).entrepriseId(2).email("d@acme.fr").nom("Durand")
                .formuleDePolitesse(2).build();
        when(contactRepository.findAllByEntrepriseIdInAndTitulaire(List.of(1, 2), Titulaire.LAURENT))
                .thenReturn(List.of(c));

        List<EntrepriseApi> res = service.rechercherEntreprises("ac");

        assertThat(res).hasSize(2);
        assertThat(res.get(0).contacts()).isEmpty();
        assertThat(res.get(1).contacts()).singleElement().satisfies(ca -> {
            assertThat(ca.id()).isEqualTo(7L);
            assertThat(ca.email()).isEqualTo("d@acme.fr");
        });
        verify(contactRepository, times(1)).findAllByEntrepriseIdInAndTitulaire(any(), any());
    }

    @Test
    void creer_entrepriseExistanteActive_contactExistant() {
        when(entrepriseService.getEntreprise(12)).thenReturn(entreprise(12, true));
        when(contactService.getContact(8L)).thenReturn(Contact.builder().id(8L).entrepriseId(12).build());
        when(annonceCreationService.creerAvecLettre(any(), anyString())).thenReturn(annonce(5L, 1));

        CandidatureReponse r = service.creer(demande(
                new EntrepriseDemande(12, null, null, null, null, null),
                new ContactDemande(8L, null, null, null)));

        assertThat(r.entrepriseCreee()).isFalse();
        assertThat(r.entrepriseReactivee()).isFalse();
        assertThat(r.contactCree()).isFalse();
        assertThat(r.annonceId()).isEqualTo(5L);
        assertThat(r.contactId()).isEqualTo(8L);
        assertThat(r.statut()).isEqualTo(1);
        verify(entrepriseService, never()).setActive(anyInt(), anyBoolean());
    }

    @Test
    void creer_entrepriseInactive_estReactivee() {
        when(entrepriseService.getEntreprise(12)).thenReturn(entreprise(12, false));
        when(annonceCreationService.creerAvecLettre(any(), anyString())).thenReturn(annonce(5L, 2));

        CandidatureReponse r = service.creer(demande(
                new EntrepriseDemande(12, null, null, null, null, null), null));

        verify(entrepriseService).setActive(12, true);
        assertThat(r.entrepriseReactivee()).isTrue();
        assertThat(r.entrepriseCreee()).isFalse();
    }

    @Test
    void creer_nouvelleEntreprise_etNouveauContact() {
        when(entrepriseService.save(any(EntrepriseForm.class))).thenReturn(31);
        when(contactService.saveContact(any(ContactForm.class), eq(31))).thenReturn(77L);
        when(annonceCreationService.creerAvecLettre(any(), anyString())).thenReturn(annonce(5L, 1));

        CandidatureReponse r = service.creer(demande(
                new EntrepriseDemande(null, "Nouvelle", null, null, "75001", "Paris"),
                new ContactDemande(null, "d@n.fr", "Durand", 2)));

        assertThat(r.entrepriseId()).isEqualTo(31);
        assertThat(r.contactId()).isEqualTo(77L);
        assertThat(r.entrepriseCreee()).isTrue();
        assertThat(r.contactCree()).isTrue();
        verify(entrepriseService, never()).setActive(anyInt(), anyBoolean());
    }

    @Test
    void creer_sansContact_annonceSite() {
        when(entrepriseService.getEntreprise(12)).thenReturn(entreprise(12, true));
        when(annonceCreationService.creerAvecLettre(any(), anyString())).thenReturn(annonce(5L, 2));

        CandidatureReponse r = service.creer(demande(
                new EntrepriseDemande(12, null, null, null, null, null), null));

        ArgumentCaptor<AnnonceForm> cap = ArgumentCaptor.forClass(AnnonceForm.class);
        verify(annonceCreationService).creerAvecLettre(cap.capture(), eq("<p>{{POLITESSE}}</p>"));
        assertThat(cap.getValue().getContactId()).isNull();
        assertThat(r.contactId()).isNull();
        assertThat(r.statut()).isEqualTo(2);
    }

    @Test
    void creer_contactDUneAutreEntreprise_404() {
        when(entrepriseService.getEntreprise(12)).thenReturn(entreprise(12, true));
        when(contactService.getContact(8L)).thenReturn(Contact.builder().id(8L).entrepriseId(99).build());

        assertThatThrownBy(() -> service.creer(demande(
                new EntrepriseDemande(12, null, null, null, null, null),
                new ContactDemande(8L, null, null, null))))
                .isInstanceOf(NoSuchElementException.class);
        verify(annonceCreationService, never()).creerAvecLettre(any(), anyString());
        verify(entrepriseService, never()).save(any());
        verify(entrepriseService, never()).setActive(anyInt(), anyBoolean());
        verify(contactService, never()).saveContact(any(), anyInt());
    }

    @Test
    void creer_nouvelleEntrepriseSansVille_refusee() {
        assertThatThrownBy(() -> service.creer(demande(
                new EntrepriseDemande(null, "Nouvelle", null, null, "75001", null), null)))
                .isInstanceOf(MetierException.class)
                .hasMessageContaining("La ville est obligatoire.");
        verify(entrepriseService, never()).save(any());
    }

    @Test
    void creer_typeContenuToujoursIa() {
        when(entrepriseService.getEntreprise(12)).thenReturn(entreprise(12, true));
        when(annonceCreationService.creerAvecLettre(any(), anyString())).thenReturn(annonce(5L, 2));

        service.creer(demande(new EntrepriseDemande(12, null, null, null, null, null), null));

        ArgumentCaptor<AnnonceForm> cap = ArgumentCaptor.forClass(AnnonceForm.class);
        verify(annonceCreationService).creerAvecLettre(cap.capture(), anyString());
        assertThat(cap.getValue().getContenuId()).isEqualTo(2);
        assertThat(cap.getValue().getEntrepriseId()).isEqualTo(12);
        assertThat(cap.getValue().getTypeAnnonce()).isEqualTo(1);
    }

    @Test
    void creer_nouveauContactEmailInvalide_refuseSansEcriture() {
        when(entrepriseService.getEntreprise(12)).thenReturn(entreprise(12, false));

        assertThatThrownBy(() -> service.creer(demande(
                new EntrepriseDemande(12, null, null, null, null, null),
                new ContactDemande(null, "pas-un-email", "Durand", 2))))
                .isInstanceOf(MetierException.class);
        verify(entrepriseService, never()).setActive(anyInt(), anyBoolean());
        verify(contactService, never()).saveContact(any(), anyInt());
    }

    @Test
    void creer_plusieursViolations_jointesParPointVirgule() {
        assertThatThrownBy(() -> service.creer(demande(
                new EntrepriseDemande(null, " ", null, null, "75001", " "), null)))
                .isInstanceOf(MetierException.class)
                .hasMessageContaining("La ville est obligatoire.")
                .hasMessageContaining("Le nom est obligatoire.")
                .hasMessageContaining(" ; ");
    }

    @Test
    void creer_contactInconnu_404SansEcriture() {
        when(entrepriseService.getEntreprise(12)).thenReturn(entreprise(12, false));
        when(contactService.getContact(8L)).thenThrow(new NoSuchElementException());

        assertThatThrownBy(() -> service.creer(demande(
                new EntrepriseDemande(12, null, null, null, null, null),
                new ContactDemande(8L, null, null, null))))
                .isInstanceOf(NoSuchElementException.class);
        verify(entrepriseService, never()).setActive(anyInt(), anyBoolean());
        verify(annonceCreationService, never()).creerAvecLettre(any(), anyString());
    }

    @Test
    void creer_contactExistantAvecNouvelleEntreprise_404SansEcriture() {
        when(contactService.getContact(8L)).thenReturn(Contact.builder().id(8L).entrepriseId(12).build());

        assertThatThrownBy(() -> service.creer(demande(
                new EntrepriseDemande(null, "Nouvelle", null, null, "75001", "Paris"),
                new ContactDemande(8L, null, null, null))))
                .isInstanceOf(NoSuchElementException.class);
        verify(entrepriseService, never()).save(any());
    }

    @Test
    void creer_posteEtReferenceTransmis_politesseNulleParDefaut0() {
        when(entrepriseService.save(any(EntrepriseForm.class))).thenReturn(31);
        when(contactService.saveContact(any(ContactForm.class), eq(31))).thenReturn(77L);
        when(annonceCreationService.creerAvecLettre(any(), anyString())).thenReturn(annonce(5L, 1));

        service.creer(demande(new EntrepriseDemande(null, "Nouvelle", null, null, "75001", "Paris"),
                new ContactDemande(null, "d@n.fr", "Durand", null)));

        ArgumentCaptor<AnnonceForm> cap = ArgumentCaptor.forClass(AnnonceForm.class);
        verify(annonceCreationService).creerAvecLettre(cap.capture(), anyString());
        assertThat(cap.getValue().getPoste()).isEqualTo("Dev");
        assertThat(cap.getValue().getReference()).isEqualTo("R1");
        assertThat(cap.getValue().getContactId()).isEqualTo(77L);
        ArgumentCaptor<ContactForm> cf = ArgumentCaptor.forClass(ContactForm.class);
        verify(contactService).saveContact(cf.capture(), eq(31));
        assertThat(cf.getValue().getFormuleDePolitesse()).isZero();
    }
}
