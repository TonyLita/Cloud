create database route;
\c route;

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
    login_attempts int default 0,
    id_locked int default 0,
    created_at timestamp default current_timestamp
);


-- Table des utilisateurs bloqués
CREATE TABLE userbloc (
    id serial primary key,
    user_id int unique not null references users(id) on delete cascade,
    blocked_at timestamp default current_timestamp,
    reason varchar(255) default 'Trop de tentatives de connexion'
);


-- 3. Création de la table pour les paramètres système (Durée session, etc.)
CREATE TABLE IF NOT EXISTS system_configs (
    config_key VARCHAR(255) PRIMARY KEY,
    config_value VARCHAR(255)
);


-- mampiditra admin user : 
INSERT INTO users (email, password_hash, role_id)
VALUES ('Lita@gmail.com', '123', 1)
ON CONFLICT (email) DO NOTHING;

INSERT INTO system_configs (config_key, config_value) 
VALUES ('session_timeout', '30')
ON CONFLICT (config_key) DO NOTHING;
