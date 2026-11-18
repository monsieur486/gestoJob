package com.mr486.gestojob.dto.api;

/**
 * Modèle de lettre de motivation exposé par l'API : sa clé, son contenu HTML
 * (avec les variables {@code {{POLITESSE}}} et {@code {{POSTE}}}) et le poste
 * proposé par défaut pour ce titulaire.
 *
 * <p><b>Exemple :</b> {@code new ModeleLettreApi("CONTENU_IA", "<p>{{POLITESSE}}</p>", "Développeur Java")}.</p>
 *
 * @param cle            clé du modèle (nom de {@code CleModele})
 * @param html           contenu HTML du modèle
 * @param posteParDefaut intitulé de poste par défaut du titulaire
 */
public record ModeleLettreApi(String cle, String html, String posteParDefaut) {
}
