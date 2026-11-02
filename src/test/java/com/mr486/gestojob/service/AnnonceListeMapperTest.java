package com.mr486.gestojob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.CleModele;
import com.mr486.gestojob.model.Entreprise;
import com.mr486.gestojob.model.Profil;
import com.mr486.gestojob.model.Titulaire;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnonceListeMapperTest {

    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private ContactService contactService;
    @Mock
    private LibelleService libelleService;
    @Mock
    private ProfilService profilService;

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

    @Test
    void toAnnonceListe_afficheLeLibelleDuProfilDuTitulaire_resoluUneSeuleFoisParLot() {
        Profil profil = Profil.builder().titulaire(Titulaire.RENEE).build();
        Annonce a1 = Annonce.builder().id(1L).entrepriseId(3).typeAnnonce(0).typeContenu(2)
                .titulaire(Titulaire.RENEE).build();
        Annonce a2 = Annonce.builder().id(2L).entrepriseId(3).typeAnnonce(0).typeContenu(2)
                .titulaire(Titulaire.RENEE).build();
        when(entrepriseService.getEntreprisesByIds(anyCollection()))
                .thenReturn(Map.of(3, Entreprise.builder().id(3).nom("Acme").build()));
        when(contactService.getContactsByIds(anyCollection())).thenReturn(Map.of());
        when(libelleService.construitLibelle(any())).thenReturn("Objet");
        when(profilService.getProfil(Titulaire.RENEE)).thenReturn(profil);
        when(profilService.libelleContenu(profil, CleModele.CONTENU_IA)).thenReturn("Lettre 3");

        List<AnnonceListe> resultat = mapper.toAnnonceListe(List.of(a1, a2));

        assertThat(resultat.get(0).getInfo()).endsWith(" Lettre 3").doesNotContain("IA");
        assertThat(resultat.get(1).getInfo()).endsWith(" Lettre 3");
        verify(profilService, times(1)).getProfil(Titulaire.RENEE);
    }

    @Test
    void toAnnonceListe_sansTitulaire_neChargeAucunProfil_etNAfficheAucunLibelle() {
        Annonce annonce = Annonce.builder().id(1L).entrepriseId(3).typeAnnonce(0).typeContenu(1).build();
        when(entrepriseService.getEntreprisesByIds(anyCollection()))
                .thenReturn(Map.of(3, Entreprise.builder().id(3).nom("Acme").build()));
        when(contactService.getContactsByIds(anyCollection())).thenReturn(Map.of());

        List<AnnonceListe> resultat = mapper.toAnnonceListe(List.of(annonce));

        assertThat(resultat.get(0).getInfo()).endsWith("🌐site").doesNotContain("MS");
        verify(profilService, never()).getProfil(any());
    }
}
