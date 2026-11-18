package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.OffreConsulteeForm;
import com.mr486.gestojob.dto.api.OffreConsulteeDemande;
import com.mr486.gestojob.dto.api.OffreConsulteeReponse;
import com.mr486.gestojob.exception.MetierException;
import com.mr486.gestojob.model.OffreConsultee;
import com.mr486.gestojob.model.Titulaire;
import com.mr486.gestojob.persistance.OffreConsulteeRepository;
import com.mr486.gestojob.securite.UtilisateurCourant;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Vérifie que le journal des offres consultées est toujours lu, écrit et
 * supprimé pour le titulaire connecté, et qu'un id de l'autre titulaire se
 * comporte comme un id inconnu.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OffreConsulteeServiceTest {

    @Mock
    private OffreConsulteeRepository repository;
    @Mock
    private UtilisateurCourant utilisateurCourant;

    @InjectMocks
    private OffreConsulteeService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAnnoncesParPage", 10);
        when(utilisateurCourant.titulaire()).thenReturn(Titulaire.RENEE);
        when(repository.save(any(OffreConsultee.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static OffreConsulteeForm form() {
        OffreConsulteeForm form = new OffreConsulteeForm();
        form.setDateConsultation(LocalDate.of(2026, 11, 18));
        form.setIntitule("  Dév Java ");
        form.setEntreprise(" Acme ");
        form.setLien("  ");
        form.setMotifEcart(2);
        form.setCommentaire("");
        return form;
    }

    @Test
    void lister_filtreParTitulaireEtPagine() {
        when(repository.findAllByTitulaireOrderByDateConsultationDescIdDesc(any(), any())).thenReturn(Page.empty());

        service.lister(2);
        verify(repository).findAllByTitulaireOrderByDateConsultationDescIdDesc(Titulaire.RENEE, PageRequest.of(2, 10));

        service.lister(-1);
        verify(repository).findAllByTitulaireOrderByDateConsultationDescIdDesc(Titulaire.RENEE, PageRequest.of(0, 10));
    }

    @Test
    void ajouter_affecteLeTitulaireEtNettoie() {
        service.ajouter(form());

        ArgumentCaptor<OffreConsultee> captor = ArgumentCaptor.forClass(OffreConsultee.class);
        verify(repository).save(captor.capture());
        OffreConsultee enregistree = captor.getValue();
        assertThat(enregistree.getTitulaire()).isEqualTo(Titulaire.RENEE);
        assertThat(enregistree.getIntitule()).isEqualTo("Dév Java");
        assertThat(enregistree.getEntreprise()).isEqualTo("Acme");
        assertThat(enregistree.getLien()).isNull();
        assertThat(enregistree.getCommentaire()).isNull();
        assertThat(enregistree.getMotifEcart()).isEqualTo(2);
        assertThat(enregistree.getDateConsultation()).isEqualTo(LocalDate.of(2026, 11, 18));
        assertThat(enregistree.getId()).isNull();
    }

    @Test
    void ajouter_conserveLienEtCommentaireRenseignes() {
        OffreConsulteeForm form = form();
        form.setLien(" https://exemple.fr/offre ");
        form.setCommentaire(" Lyon ");

        OffreConsultee enregistree = service.ajouter(form);

        assertThat(enregistree.getLien()).isEqualTo("https://exemple.fr/offre");
        assertThat(enregistree.getCommentaire()).isEqualTo("Lyon");
    }

    @Test
    void supprimer_entreeDuTitulaire_supprime() {
        OffreConsultee entree = OffreConsultee.builder().id(5L).titulaire(Titulaire.RENEE).build();
        when(repository.findByIdAndTitulaire(5L, Titulaire.RENEE)).thenReturn(Optional.of(entree));

        service.supprimer(5L);

        verify(repository).delete(entree);
    }

    @Test
    void supprimer_entreeInconnueOuDeLAutreTitulaire_leveNoSuchElement() {
        when(repository.findByIdAndTitulaire(6L, Titulaire.RENEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimer(6L)).isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void ajouterDepuisApi_motifEnMinuscules_etSansDate() {
        when(repository.save(any(OffreConsultee.class))).thenAnswer(i -> {
            OffreConsultee o = i.getArgument(0);
            o.setId(12L);
            return o;
        });

        OffreConsulteeReponse reponse = service.ajouterDepuisApi(
                new OffreConsulteeDemande("Tech lead", "Foo SA", null, "distance", "Lyon", null));

        ArgumentCaptor<OffreConsultee> captor = ArgumentCaptor.forClass(OffreConsultee.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMotifEcart()).isEqualTo(3);
        assertThat(captor.getValue().getDateConsultation()).isEqualTo(LocalDate.now());
        assertThat(reponse).isEqualTo(new OffreConsulteeReponse(12L, LocalDate.now(), "DISTANCE"));
    }

    @Test
    void ajouterDepuisApi_dateFournie_estConservee() {
        OffreConsulteeReponse reponse = service.ajouterDepuisApi(
                new OffreConsulteeDemande("Tech lead", "Foo SA", null, "PROFIL", null, LocalDate.of(2026, 11, 2)));

        assertThat(reponse.dateConsultation()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(reponse.motif()).isEqualTo("PROFIL");
    }

    @Test
    void ajouterDepuisApi_motifInconnu_leveMetierException() {
        OffreConsulteeDemande demande = new OffreConsulteeDemande("Tech lead", "Foo SA", null, "AUTRE", null, null);

        assertThatThrownBy(() -> service.ajouterDepuisApi(demande))
                .isInstanceOf(MetierException.class)
                .hasMessage("Motif inconnu : attendu PROFIL, POSTE ou DISTANCE.");
        verify(repository, never()).save(any());
    }
}
