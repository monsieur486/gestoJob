-- Aligne la colonne date_envoi sur le type Java OffsetDateTime.
-- Les valeurs naïves existantes sont interprétées comme de l'UTC (le driver
-- PostgreSQL normalise déjà les OffsetDateTime en UTC à l'écriture).
ALTER TABLE annonces
    ALTER COLUMN date_envoi TYPE TIMESTAMP WITH TIME ZONE
        USING date_envoi AT TIME ZONE 'UTC';
