package com.example.cloud.controller;

import com.example.cloud.entity.User;
import com.example.cloud.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
@Tag(name = "User Controller", description = "Gestion des utilisateurs et authentification")
public class UserController {

    private final UserService userService;
    private final com.example.cloud.service.SystemConfigService systemConfigService;

    public UserController(UserService userService, com.example.cloud.service.SystemConfigService systemConfigService) {
        this.userService = userService;
        this.systemConfigService = systemConfigService;
    }

    @Operation(summary = "Récupérer la configuration de la durée de session")
    @GetMapping("/config/timeout")
    public ResponseEntity<?> getSessionTimeout() {
        return ResponseEntity.ok(systemConfigService.getSessionTimeoutMinutes());
    }

    @Operation(summary = "Modifier la configuration de la durée de session (Admin uniquement)")
    @PutMapping("/config/timeout")
    public ResponseEntity<?> updateSessionTimeout(@RequestBody java.util.Map<String, Integer> body, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null || loggedInUser.getRole().getId() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        
        try {
            int minutes = body.get("minutes");
            systemConfigService.updateSessionTimeout(minutes);
            return ResponseEntity.ok("Session timeout updated to " + minutes + " minutes");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update timeout: " + e.getMessage());
        }
    }

    @Operation(summary = "Récupérer tous les utilisateurs (Admin uniquement)")
    @GetMapping
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null || loggedInUser.getRole().getId() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Modifier un utilisateur")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        // Autoriser si Admin OU si c'est son propre compte
        if (loggedInUser.getRole().getId() != 1 && !loggedInUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only modify your own profile");
        }

        try {
            // Dans cet exemple, passwordHash contient le nouveau mot de passe clair
            User updated = userService.updateUser(id, userDetails, userDetails.getPasswordHash());
            // Update session if user modified themselves
            if (loggedInUser.getId().equals(id)) {
                session.setAttribute("user", updated);
            }
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Update failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Supprimer un utilisateur")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        // Autoriser si Admin OU si c'est son propre compte
        if (loggedInUser.getRole().getId() != 1 && !loggedInUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own profile");
        }

        try {
            userService.deleteUser(id);
            if (loggedInUser.getId().equals(id)) {
                session.invalidate();
            }
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Deletion failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Inscription d'un nouvel utilisateur")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // Note: in a real app, use a DTO to get the plain password
            // and the user object. For this example, we use the passwordHash field as the clear password
            // for the Firebase call.
            String clearPassword = user.getPasswordHash(); 
            
            // On devrait hasher le mot de passe avant de l'envoyer au service si on veut le stocker hashé en DB
            // Mais ici le service va s'en charger ou utiliser le clair pour Firebase.
            User registeredUser = userService.registerUser(user, clearPassword);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Récupérer la liste des utilisateurs bloqués (Admin uniquement)")
    @GetMapping("/blocked")
    public ResponseEntity<?> getBlockedUsers(HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null || loggedInUser.getRole().getId() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.ok(userService.findAllBlocked());
    }

    @Operation(summary = "Débloquer un utilisateur (Admin uniquement)")
    @PutMapping("/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null || loggedInUser.getRole().getId() != 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            userService.unlockUser(id);
            return ResponseEntity.ok("User unlocked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to unlock: " + e.getMessage());
        }
    }

    @Operation(summary = "Connexion utilisateur et création de session")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest, HttpSession session) {
        Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Vérifier si le compte est bloqué
            if (user.getIdLocked() != null && user.getIdLocked() == 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Compte bloqué. Contactez un administrateur.");
            }

            if (user.getPasswordHash().equals(loginRequest.getPasswordHash())) {
                // Succès : réinitialiser les tentatives
                userService.resetLoginAttempts(user);
                session.setAttribute("user", user);
                return ResponseEntity.ok(user);
            } else {
                // Échec : incrémenter le compteur
                userService.processFailedLogin(user);
                return ResponseEntity.status(401).body("Identifiants invalides");
            }
        }
        return ResponseEntity.status(401).body("Identifiants invalides");
    }

    @Operation(summary = "Récupérer l'utilisateur connecté via session")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(401).body("Not authenticated");
    }

    @Operation(summary = "Déconnexion et destruction de session")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
