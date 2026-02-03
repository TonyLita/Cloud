Résumé de l’architecture recommandée

Source de vérité primaire : PostgreSQL (web backend). Lors d’une inscription web, créer l’utilisateur en Postgres ET créer l’utilisateur Firebase via Firebase Admin SDK (pour login mobile/web online).
Login online : mobile et web utilisent Firebase Authentication (email/password) — Firebase émet token JWT que le backend peut vérifier.
Login offline : mobile utilise une copie locale chiffrée de l’utilisateur (Postgres local ou DB embarquée). Lors du premier login online, synchroniser l’enregistrement utilisateur (id, email, password_hash, salt, dernier_sync, firebase_uid) vers la DB locale. Vérifier le mot de passe en local via le même algorithme de hachage (bcrypt/argon2).
Sync & sécurité : chiffrer la DB locale (device keystore), limiter durée d’utilisation offline (ex: 7 jours), forcer re-login online périodiquement, et éviter stockage de tokens persistants non chiffrés.

# La communication est indirecte et se fait via le Firebase Admin SDK intégré à Spring Boot :
1. Inscription : Le client envoie les données au backend. Spring Boot enregistre l'utilisateur dans PostgreSQL, puis appelle Firebase pour y créer le même utilisateur. On récupère le uid Firebase pour le stocker dans PostgreSQL.

2. Authentification : Le mobile s'authentifie auprès de Firebase, reçoit un JWT, l'envoie à Spring Boot, qui vérifie l'existence de l'utilisateur dans PostgreSQL via le uid.
   

# Structure des données Firebase:
     Firebase se divise en deux parties pour votre cas :

1.  Firebase Authentication : 

C'est un service opaque (pas de table SQL).
Contient : Email, Password (hashé par Google), UID.
C'est ce qui permet le login "Online" sur mobile.

2. Cloud Firestore (Optionnel, pour synchronisation de données) :

Si vous voulez synchroniser des profils, vous créez une collection users.

Structure NoSQL (documents) :
        {
  "users": {
    "FIREBASE_UID_123": {
      "email": "user@example.com",
      "postgres_id": 1,
      "created_at": "2026-01-31T10:00:00Z"
    }
  }
}





