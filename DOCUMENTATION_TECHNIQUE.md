# Documentation Technique - Projet Cloud

## 1. Vue d'ensemble de l'architecture

### Architecture du système (Diagramme textuel)

```
┌─────────────────────────────────────────────────────────────────┐
│                        COUCHE CLIENT                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Frontend Vue.js 3 (Port 8081)                           │  │
│  │  - Login.vue                                             │  │
│  │  - RegisterProfile.vue                                   │  │
│  │  - Accueil.vue (Dashboard)                               │  │
│  │  - Vue Router + Axios HTTP Client                        │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────────┘
                       │ API REST HTTP (CORS activé)
                       │ Cookies de session
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│                    COUCHE APPLICATION                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Backend Spring Boot 3.2.2 (Port 8080)                   │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Contrôleurs (Endpoints REST)                      │  │  │
│  │  │  - UserController: /api/users/**                   │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Services (Logique métier)                         │  │  │
│  │  │  - UserService: CRUD, Auth, Sync Firebase          │  │  │
│  │  │  - SystemConfigService: Gestion config             │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Repositories (Accès données)                      │  │  │
│  │  │  - UserRepository, RoleRepository                  │  │  │
│  │  │  - UserBlocRepository, SystemConfigRepository     │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │  Configuration & Filtres                           │  │  │
│  │  │  - FirebaseConfig: Init Firebase Admin SDK        │  │  │
│  │  │  - DynamicSessionTimeoutFilter: Contrôle session  │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────┬─────────────────────────┬───────────────────────┘
                │                         │
                │ JPA/Hibernate           │ Firebase Admin SDK
                │                         │
                ↓                         ↓
┌─────────────────────────────┐   ┌──────────────────────────────┐
│     COUCHE DONNÉES          │   │   FIREBASE CLOUD             │
│  ┌───────────────────────┐  │   │  ┌────────────────────────┐ │
│  │  PostgreSQL (5432)    │  │   │  │ Firebase Auth          │ │
│  │  - users              │  │   │  │ - Authentification     │ │
│  │  - roles              │  │   │  └────────────────────────┘ │
│  │  - userbloc           │  │   │  ┌────────────────────────┐ │
│  │  - system_configs     │  │   │  │ Cloud Firestore        │ │
│  └───────────────────────┘  │   │  │ - Collection users     │ │
└─────────────────────────────┘   │  │ - Collection configs   │ │
                                  │  └────────────────────────┘ │
                                  └──────────────────────────────┘
```

---

## 2. Stack technique

### Frontend
- **Framework:** Vue.js 3.2.13
- **Router:** Vue Router 4.0
- **Client HTTP:** Axios 1.6.7
- **Build:** Vue CLI 5.0
- **Port:** 8081

### Backend
- **Framework:** Spring Boot 3.2.2
- **Langage:** Java 19
- **Build:** Maven
- **ORM:** Hibernate/JPA
- **Documentation:** SpringDoc OpenAPI 2.3.0
- **Port:** 8080

### Base de données & Cloud
- **SGBD:** PostgreSQL (Port 5432, DB: `route`)
- **Cloud:** Google Firebase
  - Firebase Authentication
  - Cloud Firestore
- **SDK:** Firebase Admin SDK 9.2.0

### Sécurité
- Sessions HTTP avec cookies
- Timeout de session dynamique (configurable)
- Verrouillage après 3 tentatives échouées
- CORS activé pour localhost:8081

---

## 3. Schéma de la base de données

### Tables & Relations

#### Table `roles`
```sql
id (PK, serial) | label (varchar)
1               | Admin
2               | Utilisateur
```

#### Table `users`
```sql
id (PK, serial)
email (UNIQUE, varchar(50))
password_hash (varchar(255))
firebase_uid (UNIQUE, varchar(128))
role_id (FK → roles.id, default: 2)
login_attempts (int, default: 0)
id_locked (int, default: 0)  -- 0: Déverrouillé, 1: Verrouillé
created_at (timestamp)
```

#### Table `userbloc`
```sql
id (PK, serial)
user_id (FK → users.id, UNIQUE, ON DELETE CASCADE)
blocked_at (timestamp)
reason (varchar(255))
```

#### Table `system_configs`
```sql
config_key (PK, varchar(255))
config_value (varchar(255))

Exemple: ('session_timeout', '30')
```

### Relations entre entités
- **User → Role:** Many-to-One
- **UserBloc → User:** One-to-One
- **SystemConfig:** Clé-valeur (pas de relation)

---

## 4. Architecture Backend

### Entités (JPA)

#### `User.java`
- Mappage vers table `users`
- **Annotations:** `@Entity`, `@Table`, `@Getter`, `@Setter`
- **Champs clés:**
  - `firebaseUid`: Lien vers Firebase Auth
  - `loginAttempts`: Suivi des tentatives
  - `idLocked`: État de verrouillage

#### `UserBloc.java`
- Mappage vers table `userbloc`
- Relation `@OneToOne` avec User
- Stocke la raison et l'horodatage

### Repositories (Spring Data JPA)

#### UserRepository
- `findByEmail(String)`: Recherche par email
- `findByFirebaseUid(String)`: Recherche par UID Firebase

#### UserBlocRepository
- `findByUserId(Long)`: Vérifie si bloqué
- `deleteByUserId(Long)`: Supprime l'enregistrement

### Services (Logique métier)

#### UserService
**Méthodes principales:**

- **`registerUser`:**
  1. Sauvegarde dans PostgreSQL
  2. Création compte Firebase Auth
  3. Synchronisation avec Firestore
  4. Retourne l'utilisateur avec `firebase_uid`

- **`processFailedLogin`:**
  - Incrémente `login_attempts`
  - Verrouille après 3 échecs
  - Crée l'enregistrement `UserBloc`

- **`updateUser`:**
  - Mise à jour Firebase Auth (email/mot de passe)
  - Mise à jour Firestore
  - Mise à jour PostgreSQL

- **`unlockUser`:**
  - Réinitialise le statut de verrouillage
  - Supprime l'enregistrement de blocage

#### SystemConfigService
- **`getSessionTimeoutMinutes`:** Récupère le timeout (défaut: 30)
- **`updateSessionTimeout`:**
  - Mise à jour PostgreSQL
  - Synchronisation avec Firestore

### Contrôleurs (API REST)

#### UserController
Base: `/api/users`

**Endpoints d'authentification:**
- `POST /login` - Connexion avec création de session
- `POST /logout` - Destruction de session
- `GET /me` - Utilisateur actuel
- `POST /register` - Inscription

**Gestion des utilisateurs:**
- `GET /` - Liste tous les utilisateurs (Admin)
- `PUT /{id}` - Modifier utilisateur (Admin ou soi-même)
- `DELETE /{id}` - Supprimer utilisateur (Admin ou soi-même)

**Fonctionnalités Admin:**
- `GET /blocked` - Liste des utilisateurs bloqués
- `PUT /{id}/unlock` - Débloquer un utilisateur
- `GET /config/timeout` - Configuration timeout
- `PUT /config/timeout` - Modifier timeout

**Autorisation:**
- Vérification Admin: `user.getRole().getId() == 1`
- Auto-modification autorisée

### Configuration

#### application.properties
```properties
# Connexion PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/route
spring.datasource.username=postgres
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update

# Gestion session
server.servlet.session.timeout=30m
server.servlet.session.cookie.max-age=1800
```

#### FirebaseConfig.java
- Initialisation Firebase Admin SDK
- Chargement `firebase-service-account.json`

#### DynamicSessionTimeoutFilter.java
- Implémente `Filter`
- Intercepte toutes les requêtes
- Applique dynamiquement le timeout depuis la DB

---

## 5. Architecture Frontend

### Composants Vue.js

#### Login.vue
- **But:** Formulaire d'authentification
- **API:** `POST /api/users/login`
- **Fonctionnalités:**
  - Formulaire email/mot de passe
  - Affichage erreurs (compte bloqué)
  - Gestion cookies de session
  - Redirection vers `/accueil`

#### RegisterProfile.vue
- **But:** Inscription nouvel utilisateur
- **API:** `POST /api/users/register`
- **Fonctionnalités:**
  - Création simultanée Firebase + PostgreSQL
  - Messages de succès/erreur

#### Accueil.vue (Dashboard)
- **But:** Tableau de bord principal

**Pour tous les utilisateurs:**
- Affichage profil (email, rôle)
- Modification profil
- Suppression compte
- Déconnexion
- Auto-logout (vérification 60s)

**Fonctionnalités Admin uniquement:**
- Liste de tous les utilisateurs
- Modification/suppression de n'importe quel utilisateur
- Liste des utilisateurs bloqués
- Déblocage de comptes
- Configuration timeout de session

**Appels API:**
- `GET /api/users/me` - Validation session
- `GET /api/users` - Liste (Admin)
- `GET /api/users/blocked` - Bloqués (Admin)
- `PUT /api/users/{id}` - Modification
- `DELETE /api/users/{id}` - Suppression
- `PUT /api/users/{id}/unlock` - Déblocage
- `GET/PUT /api/users/config/timeout` - Configuration

### Configuration Router
```javascript
Routes:
  /          → Redirect vers /login
  /login     → Login.vue
  /register  → RegisterProfile.vue
  /accueil   → Accueil.vue
```

### Pattern d'intégration API
- **URL de base:** `http://localhost:8080`
- **CORS:** `{ withCredentials: true }` (cookies)
- **Gestion erreurs:**
  - 401 → Redirect login
  - 403 → "Accès refusé"
  - 400 → Erreurs de validation

---

## 6. Flux de données

### Flux d'inscription
```
1. Frontend → POST /api/users/register {email, passwordHash}
2. Backend UserService:
   a. Sauvegarde PostgreSQL (role_id=2)
   b. Création Firebase Auth
   c. Récupération Firebase UID
   d. Création document Firestore: users/{firebaseUid}
   e. Mise à jour PostgreSQL avec firebaseUid
3. Réponse: User avec firebaseUid
```

### Flux de connexion
```
1. Frontend → POST /api/users/login {email, passwordHash}
2. Backend:
   a. Recherche user par email
   b. Vérification verrouillage (idLocked=1)
   c. Vérification mot de passe
   d. Succès: Reset loginAttempts, création session
   e. Échec: Incrémentation loginAttempts, verrouillage si ≥3
3. Réponse: User + cookie session
```

### Synchronisation triple
```
Modification utilisateur:
1. Frontend → PUT /api/users/{id}
2. Backend:
   a. Mise à jour Firebase Auth (email/mdp)
   b. Mise à jour Firestore (email)
   c. Mise à jour PostgreSQL
3. Cohérence garantie sur 3 systèmes
```

---

## 7. Fonctionnalités implémentées

### Authentification & Autorisation
✅ Authentification email/mot de passe  
✅ Sessions HTTP avec cookies  
✅ Contrôle d'accès basé sur les rôles  
✅ Intégration Firebase Auth  
✅ Auto-déconnexion sur expiration  

### Gestion des utilisateurs
✅ Inscription (sync Firebase + PostgreSQL)  
✅ Modification profil (self-service)  
✅ Gestion admin (CRUD complet)  
✅ Suppression compte (cascade)  

### Sécurité
✅ Suivi tentatives de connexion  
✅ Verrouillage automatique après 3 échecs  
✅ Déblocage par admin  
✅ Interface gestion bloqués  
✅ Timeout de session configurable  

### Synchronisation multi-bases
✅ PostgreSQL comme base principale  
✅ Firebase Auth pour l'authentification  
✅ Cloud Firestore pour les profils  
✅ Sync temps réel sur toutes opérations  

---

## 8. Résumé des endpoints API

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| POST | `/api/users/login` | Public | Connexion |
| POST | `/api/users/logout` | Auth | Déconnexion |
| POST | `/api/users/register` | Public | Inscription |
| GET | `/api/users/me` | Auth | Utilisateur actuel |
| GET | `/api/users` | Admin | Liste utilisateurs |
| PUT | `/api/users/{id}` | Admin/Soi | Modification |
| DELETE | `/api/users/{id}` | Admin/Soi | Suppression |
| GET | `/api/users/blocked` | Admin | Liste bloqués |
| PUT | `/api/users/{id}/unlock` | Admin | Déblocage |
| GET/PUT | `/api/users/config/timeout` | Admin | Config timeout |

---

## 9. Déploiement

### Configuration environnement
- **PostgreSQL:** Mettre à jour connexion dans `application.properties`
- **Firebase:** Remplacer `firebase-service-account.json` (production)
- **CORS:** Modifier `@CrossOrigin` avec URL frontend prod
- **Session:** Ajuster timeout pour production

### Setup base de données
```sql
CREATE DATABASE route;
\c route
-- Exécuter base.sql
```

### Build & Lancement

**Backend:**
```bash
cd Backend
mvn clean compile
mvn spring-boot:run
# http://localhost:8080
```

**Frontend:**
```bash
cd web
npm install
npm run serve
# http://localhost:8081
```

### Sécurisation production
⚠️ **CRITIQUE:** Implémenter hachage mot de passe (BCrypt)  
⚠️ Utiliser HTTPS  
⚠️ Protection CSRF  
⚠️ Rate limiting  
⚠️ Sécuriser credentials Firebase (variables env)  
⚠️ Connection pooling PostgreSQL  
⚠️ Logging et monitoring  
⚠️ Validation et sanitisation des entrées  

---

## 10. Documentation API

SpringDoc OpenAPI disponible: `http://localhost:8080/swagger-ui.html`

---

**Dernière mise à jour:** 03 Février 2026  
**Version:** 1.0  
**Projet:** Système de Gestion des Utilisateurs Cloud
