package com.mr486.gestojob.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

/**
 * Vérifie qu'une valeur non convertible (date mal formée, motif non numérique)
 * produit un message français tiré de {@code messages.properties}, et non le
 * message technique anglais de Spring (« Failed to convert property value… »).
 */
class OffreConsulteeFormConversionTest {

    @Test
    void dateMalFormee_messageFrancais() {
        assertThat(messageDuChamp("dateConsultation", "18/13/2026")).isEqualTo("La date est invalide.");
    }

    @Test
    void motifNonNumerique_messageFrancais() {
        assertThat(messageDuChamp("motifEcart", "abc")).isEqualTo("Motif inconnu.");
    }

    // Lie une seule valeur au formulaire comme le ferait Spring MVC, puis résout
    // le message de l'erreur de conversion avec le fichier messages de l'application.
    private static String messageDuChamp(String champ, String valeur) {
        DataBinder binder = new DataBinder(new OffreConsulteeForm(), "form");
        binder.setConversionService(new DefaultFormattingConversionService());
        binder.bind(new MutablePropertyValues().add(champ, valeur));

        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messages.setUseCodeAsDefaultMessage(false);
        BindingResult resultat = binder.getBindingResult();
        return messages.getMessage(resultat.getFieldError(champ), Locale.FRANCE);
    }
}
