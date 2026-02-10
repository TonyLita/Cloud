-- Initialisation de la base de données
create table roles (
    id serial primary key,
    label varchar(20) not null
);

insert into roles (id, label) values (1, 'Admin'), (2, 'Utilisateur');

create table users (
    id serial primary key,
    email varchar(50) unique not null,
    password_hash varchar(255) not null,
    firebase_uid varchar(128) unique,
    role_id int not null default 2 references roles(id),
    est_bloque boolean default false,
    tentatives_connexion integer default 0,
    notification TEXT,
    date_blocage timestamp,
    date_creation timestamp default current_timestamp,
    date_modification timestamp default current_timestamp
);


-- 3. Création de la table pour les paramètres système (Durée session, etc.)
CREATE TABLE IF NOT EXISTS system_configs (
    config_key VARCHAR(255) PRIMARY KEY,
    config_value VARCHAR(255)
);


-- mampiditra admin user : 
INSERT INTO users (email, password_hash, role_id)
VALUES ('Lita@gmail.com', 'password123', 1)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, role_id)
VALUES ('admin@gmail.com', 'password123', 1)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, role_id)
VALUES ('User@gmail.com', 'password123', 2)
ON CONFLICT (email) DO NOTHING;

INSERT INTO system_configs (config_key, config_value) 
VALUES ('session_timeout', '30')
ON CONFLICT (config_key) DO NOTHING;

-- Configuration de la carte Offline (Tana)
INSERT INTO system_configs (config_key, config_value)
VALUES
    ('tile_server_url', 'http://localhost:8085/data/antananarivo/{z}/{x}/{y}.pbf'),
    ('map_center_lat', '-18.91'),
    ('map_center_lng', '47.54'),
    ('map_zoom', '12')
ON CONFLICT (config_key) DO NOTHING;

-- Nouvelles tables pour le suivi des travaux
CREATE TABLE IF NOT EXISTS entreprise (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20),
    email VARCHAR(100),
    adresse VARCHAR(255),
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    firebase_id VARCHAR(128) UNIQUE
);

CREATE TABLE IF NOT EXISTS signalement (
    id SERIAL PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    adresse VARCHAR(255),
    description TEXT,
    statut INT DEFAULT 1 NOT NULL CHECK (statut IN (1, 2, 3)),
    surface_m2 DOUBLE PRECISION,
    niveau INT CHECK (niveau >= 1 AND niveau <= 10),
    budget DOUBLE PRECISION,
    photo_url VARCHAR(255),
    user_id INT REFERENCES users(id),
    id_entreprise INT REFERENCES entreprise(id),
    date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_en_cours TIMESTAMP,
    date_termine TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    firebase_id VARCHAR(128) UNIQUE
);

-- Table pour la configuration des prix par niveau
CREATE TABLE IF NOT EXISTS configuration (
    id SERIAL PRIMARY KEY,
    niveau INT UNIQUE NOT NULL CHECK (niveau >= 1 AND niveau <= 10),
    prix_par_m2 DOUBLE PRECISION NOT NULL,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Données initiales pour la configuration (niveaux 1 à 10)
INSERT INTO configuration (niveau, prix_par_m2) VALUES
(1, 10.0),
(2, 20.0),
(3, 30.0),
(4, 40.0),
(5, 50.0),
(6, 60.0),
(7, 70.0),
(8, 80.0),
(9, 90.0),
(10, 100.0)
ON CONFLICT (niveau) DO NOTHING;

-- Données initiales pour les entreprises
INSERT INTO entreprise (nom, telephone, email, adresse) VALUES
('Colas Madagascar', '020 22 234 56', 'contact@colas.mg', 'Ankorondrano'),
('Sogea Satom', '020 22 456 78', 'contact@sogea.mg', 'Ivato'),
('Jirama', '3547', 'clientele@jirama.mg', 'Ambohijatovo')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------
-- DONNÉES DE TEST SUPPLÉMENTAIRES (Initialisation Docker)
-- ---------------------------------------------------------

-- 1. Utilisateurs supplémentaires
INSERT INTO users (email, password_hash, role_id, notification) VALUES
('test.user@exemple.mg', 'password123', 2, 'Bienvenue sur la plateforme !'),
('jean.manager@exemple.mg', 'password123', 1, NULL)
ON CONFLICT (email) DO NOTHING;

-- Utilisateur bloqué pour test
INSERT INTO users (email, password_hash, role_id, est_bloque, tentatives_connexion, date_blocage) VALUES
('blocked@exemple.mg', 'password123', 2, true, 3, CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT (email) DO NOTHING;

-- 2. Signalements de test (Antananarivo)
-- Statut : 1=Nouveau, 2=En cours, 3=Terminé
INSERT INTO signalement (latitude, longitude, adresse, description, statut, surface_m2, niveau, budget, user_id, id_entreprise, date_signalement) VALUES
-- Nouveau signalement (Anosy)
(-18.9135, 47.5255, 'Près du Lac Anosy', 'Gros nid de poule sur la chaussée principale.', 1, 15.5, 4, 6200.0, 3, NULL, CURRENT_TIMESTAMP - INTERVAL '2 days'),
-- Nouveau signalement (Analakely)
(-18.9080, 47.5220, 'Avenue de l Indépendance', 'Dégradation du bitume devant les arcades.', 1, 25.0, 6, 15000.0, 3, NULL, CURRENT_TIMESTAMP - INTERVAL '1 day'),
-- Signalement en cours (Colas - Ivandry)
(-18.8750, 47.5260, 'Route d Ivandry', 'Réfection de la voie rapide.', 2, 150.0, 8, 120000.0, 1, 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),
-- Signalement terminé (Sogea - Ankorondrano)
(-18.8880, 47.5250, 'Zone Industrielle Ankorondrano', 'Canalisation éclatée ayant endommagé la route.', 3, 40.0, 5, 20000.0, 2, 2, CURRENT_TIMESTAMP - INTERVAL '10 days');

-- Mise à jour des dates pour les signalements en cours et terminés
UPDATE signalement SET date_en_cours = date_signalement + INTERVAL '1 day' WHERE statut >= 2;
UPDATE signalement SET date_termine = date_en_cours + INTERVAL '3 days' WHERE statut = 3;
