package com.mr486.gestojob.service;

import com.mr486.gestojob.dto.AnnonceListe;
import com.mr486.gestojob.model.Annonce;
import com.mr486.gestojob.model.Entreprise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnonceListeMapperTest {

    @Mock
    private EntrepriseService entrepriseService;
    @Mock
    private ContactService contactService;
    @Mock
    private LibelleService libelleService;

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
}
