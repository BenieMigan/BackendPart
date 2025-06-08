package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.AuthDTO;
import gestion.pac.gestionstagiairesbackend.dto.RHUserDTO;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.EmailService;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    private final EmailService emailService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenService jwtTokenService,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.emailService = emailService;
    }
    //Inscription Stagiaire
    @PostMapping("/register/stagiaire")
    public ResponseEntity<?> register(@RequestBody AuthDTO authDTO) {
        // 1. Vérifications préalables
        if (userRepository.existsByEmail(authDTO.getEmail())) {
            return ResponseEntity.badRequest().body("Email déjà utilisé");
        }

        if (authDTO.getPassword() == null || authDTO.getConfirmPassword() == null) {
            return ResponseEntity.badRequest().body("Les champs mot de passe sont obligatoires");
        }

        if (!authDTO.getPassword().equals(authDTO.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Les mots de passe ne correspondent pas");
        }

        if (authDTO.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body("Le mot de passe doit contenir au moins 8 caractères");
        }

        // 2. Création de l'utilisateur
        User user = new User();
        user.setCivilite(authDTO.getCivilite());
        user.setNom(authDTO.getNom());
        user.setPrenom(authDTO.getPrenom());
        user.setEmail(authDTO.getEmail());
        user.setPassword(passwordEncoder.encode(authDTO.getPassword()));
        user.setRole("STAGIAIRE");

        userRepository.save(user);

// Retournez un objet JSON au lieu d'une simple String
        Map<String, String> response = new HashMap<>();
        response.put("message", "Inscription réussie");
        return ResponseEntity.ok(response);
    }

    //Connexion Stagiaire
    @PostMapping("/login/stagiaire")
    public ResponseEntity<?> loginStagiaire(@RequestBody AuthDTO authDTO) {
        try {
            User user = userRepository.findByEmail(authDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

            if (!"STAGIAIRE".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Accès réservé aux stagiaires"));
            }

            if (!passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email ou mot de passe incorrect"));
            }

            String token = jwtTokenService.generateToken(user.getId());
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", user
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la connexion", "error", e.getMessage()));
        }
    }

    @PostMapping("/login/rh")
    public ResponseEntity<?> loginRH(@RequestBody AuthDTO authDTO) {
        try {
            // Trouver l'utilisateur RH
            User user = userRepository.findByEmail(authDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

            // Vérification du mot de passe
            if (!passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email ou mot de passe incorrect"));
            }

            // Vérification du rôle RH
            if (!"RH".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Non autorisé"));
            }

            // Génération du token
            String token = jwtTokenService.generateToken(user.getId());  // On passe le ID ici (Long)

            return ResponseEntity.ok(Map.of("token", token, "user", user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erreur lors de la connexion RH", "error", e.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Autoriser à la fois STAGIAIRE et RH
            if (!"STAGIAIRE".equals(user.getRole()) && !"RH".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès non autorisé");
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide");
        }
    }

    // Mot de passe oublié - Demande de réinitialisation
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Aucun compte trouvé avec cet email"));

            // Générer un token de réinitialisation (valide 1h)
            String resetToken = jwtTokenService.generatePasswordResetToken(user.getId());

            // Lien de réinitialisation
            String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;

            // Envoyer l'email réel
            emailService.ForgotPassword(user.getEmail(), "Réinitialisation du mot de passe", resetLink);

            return ResponseEntity.ok(Map.of(
                    "message", "Si un compte existe avec cet email, un lien a été envoyé"
                    // Ne pas renvoyer le token ici en production
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Réinitialisation du mot de passe
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");

            if (!newPassword.equals(confirmPassword)) {
                return ResponseEntity.badRequest().body("Les mots de passe ne correspondent pas");
            }

            Long userId = jwtTokenService.validatePasswordResetToken(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }


}