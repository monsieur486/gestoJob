package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Contact;
import com.mr486.gestojob.persistance.AnnonceRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnonceTexteServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;
    @Mock
    private ContactService contactService;
    @Mock
    private ContenuService contenuService;
    @Mock
    private LibelleService libelleService;

    @InjectMocks
    private AnnonceTexteService annonceTexteService;

    @Test
    void getMessageDePolitesse_avecContact_renvoieLaFormuleDuContact() {
        Annonce a = Annonce.builder().id(1L).contactId(5L).statusAnnonce(1).build();
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(contactService.getContact(5L)).thenReturn(
                Contact.builder().id(5L).entrepriseId(10).email("a@b.fr")
                        .formuleDePolitesse(1).nom("Durand").build());

        assertThat(annonceTexteService.getMessageDePolitesse(1L)).isEqualTo("Monsieur Durand,");
    }

    @Test
    void getMessageDePolitesse_sansContact_renvoieFormuleGenerique() {
        Annonce a = Annonce.builder().id(1L).contactId(null).statusAnnonce(2).build();
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThat(annonceTexteService.getMessageDePolitesse(1L)).isEqualTo("Madame, Monsieur,");
    }

    @Test
    void getAnnonceTxtContenuById_concateneLibelleEtContenuTexte() {
        Annonce a = Annonce.builder().id(1L).contactId(null).typeAnnonce(1)
                .reference("R1").poste("Dev").typeContenu(0).statusAnnonce(2).build();
        when(annonceRepository.findById(1L)).thenReturn(Optional.of(a));
        when(libelleService.construitLibelle(any())).thenReturn("Objet");
        when(contenuService.getTextContenu(any(), eq("Dev"), eq(0), any())).thenReturn("CORPS_TXT");

        String result = annonceTexteService.getAnnonceTxtContenuById(1L);

        assertThat(result).startsWith("Objet\n\n");
        assertThat(result).contains("CORPS_TXT");
    }
}
