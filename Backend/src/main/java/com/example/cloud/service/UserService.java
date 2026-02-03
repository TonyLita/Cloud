package com.example.cloud.service;

import com.example.cloud.entity.User;
import com.example.cloud.repository.UserRepository;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final com.example.cloud.repository.RoleRepository roleRepository;
    private final com.example.cloud.repository.UserBlocRepository userBlocRepository;

    public UserService(UserRepository userRepository, 
                       com.example.cloud.repository.RoleRepository roleRepository,
                       com.example.cloud.repository.UserBlocRepository userBlocRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userBlocRepository = userBlocRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<com.example.cloud.entity.UserBloc> findAllBlocked() {
        return userBlocRepository.findAll();
    }

    @Transactional
    public void processFailedLogin(User user) {
        int attempts = (user.getLoginAttempts() != null) ? user.getLoginAttempts() : 0;
        user.setLoginAttempts(attempts + 1);
        
        if (user.getLoginAttempts() >= 3) {
            user.setIdLocked(1);
            userBlocRepository.save(new com.example.cloud.entity.UserBloc(user, "Trop de tentatives (3)"));
        }
        userRepository.save(user);
    }

    @Transactional
    public void resetLoginAttempts(User user) {
        user.setLoginAttempts(0);
        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(Long userId) throws Exception {
        User user = userRepository.findById(userId).orElseThrow(() -> new Exception("User not found"));
        user.setIdLocked(0);
        user.setLoginAttempts(0);
        userRepository.save(user);
        userBlocRepository.deleteByUserId(userId);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User updateUser(Long id, User userDetails, String newPassword) throws Exception {
        User user = userRepository.findById(id).orElseThrow(() -> new Exception("User not found"));
        
        // Vérification : L'utilisateur doit avoir un ID Firebase pour être mis à jour sur le Cloud
        if (user.getFirebaseUid() != null && !user.getFirebaseUid().isEmpty()) {
            try {
                // 1. Update in Firebase Authentication
                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(user.getFirebaseUid())
                        .setEmail(userDetails.getEmail());
                if (newPassword != null && !newPassword.isEmpty()) {
                    request.setPassword(newPassword);
                }
                FirebaseAuth.getInstance().updateUser(request);

                // 2. Update in Cloud Firestore
                Firestore db = FirestoreClient.getFirestore();
                Map<String, Object> updates = new HashMap<>();
                updates.put("email", userDetails.getEmail());
                db.collection("users").document(user.getFirebaseUid()).update(updates).get();
            } catch (Exception e) {
                System.err.println("Firebase update failed (non-critical): " + e.getMessage());
                // On peut choisir de continuer pour mettre à jour au moins la DB locale
            }
        }

        // 3. Update in PostgreSQL
        user.setEmail(userDetails.getEmail());
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPasswordHash(newPassword);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) throws Exception {
        User user = userRepository.findById(id).orElseThrow(() -> new Exception("User not found"));

        // 1. Delete from Firebase Authentication
        FirebaseAuth.getInstance().deleteUser(user.getFirebaseUid());

        // 2. Delete from Cloud Firestore
        Firestore db = FirestoreClient.getFirestore();
        db.collection("users").document(user.getFirebaseUid()).delete().get();

        // 3. Delete from PostgreSQL
        userRepository.delete(user);
    }

    @Transactional
    public User registerUser(User user, String clearPassword) throws Exception {
        // 1. Assigner le rôle par défaut (Utilisateur = 2) si non défini
        if (user.getRole() == null) {
            user.setRole(roleRepository.findById(2).orElseThrow(() -> new Exception("Role par défaut non trouvé")));
        }

        // 2. Sauvegarder dans PostgreSQL d'abord
        User savedUser = userRepository.save(user);

        // 3. Créer l'utilisateur dans Firebase Authentication
        UserRecord.CreateRequest authRequest = new UserRecord.CreateRequest()
                .setEmail(user.getEmail())
                .setPassword(clearPassword);

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(authRequest);
        String firebaseUid = userRecord.getUid();

        // 4. Créer le profil dans Cloud Firestore pour la synchronisation
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("email", user.getEmail());
        profileData.put("postgres_id", savedUser.getId());
        profileData.put("role", savedUser.getRole().getLabel()); // Sync du rôle dans Firebase
        profileData.put("role_id", savedUser.getRole().getId());
        profileData.put("created_at", user.getCreatedAt().toString());
        profileData.put("status", "active");

        // Utiliser le UID Firebase comme ID de document Firestore
        db.collection("users").document(firebaseUid).set(profileData).get();

        // 5. Mettre à jour l'utilisateur Postgres avec le UID Firebase
        savedUser.setFirebaseUid(firebaseUid);
        return userRepository.save(savedUser);
    }

    public Optional<User> findByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
