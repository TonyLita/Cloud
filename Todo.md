Vue.js -> Spring Boot -> PostgreSQL (Local) + Firebase (Cloud).

 https://console.firebase.google.com/project/test-6c92f/firestore


 # Plan de Projet : Système de Synchronisation Hybride (Postgres + Firebase)

## 1. Architecture des Données (PostgreSQL)
- [x] **roles** : Table des privilèges (1=Admin, 2=Utilisateur).
- [x] **users** : Table centrale (Stockage local, hash passwords, meta-données sync).
- [x] **userbloc** : Table de sécurité (Archivage des comptes verrouillés).
- [x] **system_configs** : Table de configuration dynamique (Persistance des réglages).

## 2. Cœur Backend (Spring Boot)
### Entités & Repositories
- [x] `User.java` & `UserRepository` : Gestion du cycle de vie utilisateur.
- [x] `Role.java` & `RoleRepository` : Définition des permissions.
- [x] `UserBloc.java` & `UserBlocRepository` : Mapping de la sécurité.
- [x] `SystemConfig.java` & `SystemConfigRepository` : Mapping des réglages.

### Services (Logique métier)
- [x] `UserService.java` : 
    - **Rôle** : Triple synchronisation (SQL local, Firebase Auth, Firestore).
    - **Fonctions** : Inscription sync, Mise à jour cross-platform, Suppression sécurisée.
- [x] `SystemConfigService.java` :
    - **Rôle** : Synchronisation des paramètres entre la DB locale et le Cloud (Firestore).
    - **Fonctions** : Get/Set Session Timeout.

### Contrôleurs & Sécurité
- [x] `UserController.java` : API REST pour le Frontend et l'application mobile.
- [x] `FirebaseConfig.java` : Initialisation du SDK Firebase Admin.
- [x] `DynamicSessionTimeoutFilter.java` : Application en temps réel de la durée de session stockée en DB.

## 3. Interface Web (Vue.js)
- [x] `Login.vue` : Connexion session-based avec gestion des erreurs 403 (Comptes bloqués).
- [x] `RegisterProfile.vue` : Inscription avec création automatique des profils SQL et Cloud.
- [x] `Accueil.vue` :
    - **Dashboard User** : Gestion de profil.
    - **Console Admin** : Gestion des utilisateurs, déblocage des comptes `userbloc`, et modification de la durée de session.

## 4. Application Mobile (En attente)
- [ ] Interface Flutter/React Native.
- [ ] Synchronisation Offline (PostgreSQL Local sur Mobile).
- [ ] Détection de connectivité pour basculement Firebase / Local API.

---
*Dernière mise à jour : 03 Février 2026*
