-- Rattachement de chaque donnée à un titulaire ; l'existant appartient à LAURENT.
ALTER TABLE entreprises  ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE contacts     ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE annonces     ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE modele_email ADD COLUMN titulaire VARCHAR(16) NOT NULL DEFAULT 'LAURENT';
ALTER TABLE entreprises  ALTER COLUMN titulaire DROP DEFAULT;
ALTER TABLE contacts     ALTER COLUMN titulaire DROP DEFAULT;
ALTER TABLE annonces     ALTER COLUMN titulaire DROP DEFAULT;
ALTER TABLE modele_email ALTER COLUMN titulaire DROP DEFAULT;
CREATE INDEX idx_entreprises_titulaire ON entreprises (titulaire);
CREATE INDEX idx_contacts_titulaire    ON contacts (titulaire);
CREATE INDEX idx_annonces_titulaire    ON annonces (titulaire);

-- Un modèle par clé ET par titulaire.
ALTER TABLE modele_email DROP CONSTRAINT uq_modele_email_cle;
ALTER TABLE modele_email ADD CONSTRAINT uq_modele_email_titulaire_cle UNIQUE (titulaire, cle);

-- Profil : nom du candidat + libellé et poste par défaut des trois types de lettre.
CREATE TABLE profil (
    titulaire              VARCHAR(16)  NOT NULL,
    candidat_nom           VARCHAR(255) NOT NULL,
    libelle_general        VARCHAR(64)  NOT NULL,
    poste_general          VARCHAR(255) NOT NULL,
    libelle_microservices  VARCHAR(64)  NOT NULL,
    poste_microservices    VARCHAR(255) NOT NULL,
    libelle_ia             VARCHAR(64)  NOT NULL,
    poste_ia               VARCHAR(255) NOT NULL,
    CONSTRAINT pk_profil PRIMARY KEY (titulaire)
);
INSERT INTO profil VALUES
 ('LAURENT', 'Laurent Touret',
  'Général', 'de développeur Java',
  'Microservices', 'de développeur Java orienté microservices',
  'IA agentique', 'de développeur Java back-end orienté IA agentique'),
 ('RENEE', 'Renée',
  'Lettre 1', 'correspondant à mon profil',
  'Lettre 2', 'correspondant à mon profil',
  'Lettre 3', 'correspondant à mon profil');
