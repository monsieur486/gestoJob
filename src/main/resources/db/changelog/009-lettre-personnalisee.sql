-- Lettre de motivation personnalisée propre à une annonce (prioritaire sur le modèle du titulaire).
ALTER TABLE annonces ADD COLUMN lettre_personnalisee TEXT NULL;
