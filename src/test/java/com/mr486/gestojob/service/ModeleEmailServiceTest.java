package com.mr486.gestojob.service;

import com.mr486.gestojob.model.ModeleEmail;
import com.mr486.gestojob.persistance.ModeleEmailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeleEmailServiceTest {

    @Mock
    private ModeleEmailRepository modeleEmailRepository;

    @InjectMocks
    private ModeleEmailService modeleEmailService;

    @Test
    void initialiser_creeLesModelesManquants_etRemplitLeCache() {
        when(modeleEmailRepository.findByCle(any())).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.initialiser();

        // 5 clés => 5 enregistrements créés depuis les ressources par défaut
        verify(modeleEmailRepository, org.mockito.Mockito.times(5)).save(any(ModeleEmail.class));
        assertThat(modeleEmailService.getContenu("LIBELLE_SPONTANEE"))
                .contains("Candidature spontanée");
    }

    @Test
    void getContenu_cleInconnue_leveNoSuchElement() {
        assertThatThrownBy(() -> modeleEmailService.getContenu("INCONNU"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void mettreAJour_nettoieLeHtml_etMetAJourLeCache() {
        when(modeleEmailRepository.findByCle("CONTENU_GENERAL")).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.mettreAJour("CONTENU_GENERAL",
                "<p>{{POLITESSE}}</p><script>x</script>");

        String courant = modeleEmailService.getContenu("CONTENU_GENERAL");
        assertThat(courant).contains("{{POLITESSE}}").doesNotContain("script");
    }

    @Test
    void mettreAJour_libelle_neNettoiePasLeHtml_maisConserveLeTexte() {
        when(modeleEmailRepository.findByCle("LIBELLE_REFERENCE")).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.mettreAJour("LIBELLE_REFERENCE", "Réf {{REFERENCE}} - {{NOM}}");

        assertThat(modeleEmailService.getContenu("LIBELLE_REFERENCE"))
                .isEqualTo("Réf {{REFERENCE}} - {{NOM}}");
    }

    @Test
    void reinitialiser_restaureLaValeurParDefaut() {
        when(modeleEmailRepository.findByCle("LIBELLE_SPONTANEE")).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        modeleEmailService.reinitialiser("LIBELLE_SPONTANEE");

        assertThat(modeleEmailService.getContenu("LIBELLE_SPONTANEE"))
                .contains("Candidature spontanée");
    }

    @Test
    void listerModeles_retourneLesCinqModeles() {
        when(modeleEmailRepository.findByCle(any())).thenReturn(Optional.empty());
        when(modeleEmailRepository.save(any(ModeleEmail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        modeleEmailService.initialiser();

        assertThat(modeleEmailService.listerModeles()).hasSize(5);
    }
}
