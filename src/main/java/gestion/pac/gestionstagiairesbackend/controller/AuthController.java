package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.*;
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
import java.util.Random;

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

    // Endpoint pour demander une réinitialisation de mot de passe
    @PostMapping("/rh/request-password-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            // Vérifier que c'est bien l'email RH
            if (!"honfodavid29@gmail.com".equals(email)) {
                return ResponseEntity.badRequest().body("Cette fonctionnalité est réservée aux RH");
            }

            User rhUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Compte RH non trouvé"));

            // Générer un code OTP
            String otp = generateOTP();
            rhUser.setResetPasswordOtp(otp); // Ajoutez ce champ dans votre entité User
            rhUser.setOtpExpiry(LocalDateTime.now().plusMinutes(15)); // Ajoutez ce champ
            userRepository.save(rhUser);

            // Envoyer l'email avec le code OTP
            emailService.sendOTPEmail(rhUser.getEmail(), "Réinitialisation de mot de passe", otp);

            return ResponseEntity.ok(Map.of(
                    "message", "Un code OTP a été envoyé à votre adresse email"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la demande de réinitialisation", "error", e.getMessage()));
        }
    }

    // Endpoint pour vérifier l'OTP et réinitialiser le mot de passe
    @PostMapping("/rh/reset-password")
    public ResponseEntity<?> resetPasswordRH(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        try {
            User rhUser = userRepository.findByEmail("honfodavid29@gmail.com")
                    .orElseThrow(() -> new RuntimeException("Compte RH non trouvé"));

            // Vérifier l'OTP
            if (!resetPasswordDTO.getOtp().equals(rhUser.getResetPasswordOtp())) {
                return ResponseEntity.badRequest().body("Code OTP invalide");
            }

            // Vérifier l'expiration
            if (LocalDateTime.now().isAfter(rhUser.getOtpExpiry())) {
                return ResponseEntity.badRequest().body("Le code OTP a expiré");
            }

            // Mettre à jour le mot de passe
            rhUser.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
            rhUser.setResetPasswordOtp(null);
            rhUser.setOtpExpiry(null);
            userRepository.save(rhUser);

            return ResponseEntity.ok(Map.of(
                    "message", "Mot de passe réinitialisé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la réinitialisation", "error", e.getMessage()));
        }
    }

    // Endpoint pour changer le mot de passe (nécessite d'être authentifié)
    @PostMapping("/rh/change-password")
    public ResponseEntity<?> changePasswordRH(
            @RequestBody ChangePasswordDTO changePasswordDTO,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux RH");
            }

            // Vérifier l'ancien mot de passe
            if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), rhUser.getPassword())) {
                return ResponseEntity.badRequest().body("Mot de passe actuel incorrect");
            }

            // Mettre à jour le mot de passe
            rhUser.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
            userRepository.save(rhUser);

            return ResponseEntity.ok(Map.of(
                    "message", "Mot de passe changé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du changement de mot de passe", "error", e.getMessage()));
        }
    }

    private String generateOTP() {
        // Génère un OTP de 6 chiffres
        return String.format("%06d", new Random().nextInt(999999));
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

    // Endpoint pour demander une réinitialisation de mot de passe (Stagiaire)
    @PostMapping("/stagiaire/request-password-reset")
    public ResponseEntity<?> requestPasswordResetStagiaire(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            User stagiaire = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Aucun compte trouvé avec cet email"));

            // Vérifier que c'est bien un stagiaire
            if (!"STAGIAIRE".equals(stagiaire.getRole())) {
                return ResponseEntity.badRequest().body("Cette fonctionnalité est réservée aux stagiaires");
            }

            // Générer un code OTP
            String otp = generateOTP();
            stagiaire.setResetPasswordOtp(otp);
            stagiaire.setOtpExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(stagiaire);

            // Envoyer l'email avec le code OTP
            emailService.sendOTPEmail(stagiaire.getEmail(), "Réinitialisation de mot de passe", otp);

            return ResponseEntity.ok(Map.of(
                    "message", "Un code OTP a été envoyé à votre adresse email"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la demande de réinitialisation", "error", e.getMessage()));
        }
    }

    // Endpoint pour vérifier l'OTP et réinitialiser le mot de passe (Stagiaire)
    @PostMapping("/stagiaire/reset-password")
    public ResponseEntity<?> resetPasswordStagiaire(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        try {
            User stagiaire = userRepository.findByEmail(resetPasswordDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("Aucun compte trouvé avec cet email"));

            // Vérifier que c'est bien un stagiaire
            if (!"STAGIAIRE".equals(stagiaire.getRole())) {
                return ResponseEntity.badRequest().body("Cette fonctionnalité est réservée aux stagiaires");
            }

            // Vérifier l'OTP
            if (!resetPasswordDTO.getOtp().equals(stagiaire.getResetPasswordOtp())) {
                return ResponseEntity.badRequest().body("Code OTP invalide");
            }

            // Vérifier l'expiration
            if (LocalDateTime.now().isAfter(stagiaire.getOtpExpiry())) {
                return ResponseEntity.badRequest().body("Le code OTP a expiré");
            }

            // Mettre à jour le mot de passe
            stagiaire.setPassword(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
            stagiaire.setResetPasswordOtp(null);
            stagiaire.setOtpExpiry(null);
            userRepository.save(stagiaire);

            return ResponseEntity.ok(Map.of(
                    "message", "Mot de passe réinitialisé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la réinitialisation", "error", e.getMessage()));
        }
    }

    // Endpoint pour changer le mot de passe (commun RH/Stagiaire)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordDTO changePasswordDTO,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Vérifier l'ancien mot de passe
            if (!passwordEncoder.matches(changePasswordDTO.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body("Mot de passe actuel incorrect");
            }

            // Mettre à jour le mot de passe
            user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Mot de passe changé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors du changement de mot de passe", "error", e.getMessage()));
        }
    }

    // Nouvel endpoint pour la RH pour créer des comptes
    @PostMapping("/rh/create-account")
    public ResponseEntity<?> createAccountByRH(
            @RequestBody CreateAccountByRHDTO createDTO,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth RH
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Vérifications communes
            if (userRepository.existsByEmail(createDTO.getEmail())) {
                return ResponseEntity.badRequest().body("Email déjà utilisé");
            }

            if (createDTO.getPassword() == null || createDTO.getPassword().length() < 8) {
                return ResponseEntity.badRequest().body("Le mot de passe doit contenir au moins 8 caractères");
            }

            // Validation spécifique au rôle
            switch (createDTO.getRole()) {
                case "SECRETAIRE":
                    if (userRepository.existsByRoleAndDirection("SECRETAIRE", createDTO.getDirection())) {
                        return ResponseEntity.badRequest().body("Une secrétaire existe déjà pour cette direction");
                    }
                    break;

                case "CHEF_SERVICE":
                    if (userRepository.existsByRoleAndDirectionAndService("CHEF_SERVICE",
                            createDTO.getDirection(), createDTO.getService())) {
                        return ResponseEntity.badRequest().body("Un chef de service existe déjà pour ce service");
                    }
                    if (userRepository.existsByRoleAndServiceAndDirectionNot("CHEF_SERVICE",
                            createDTO.getService(), createDTO.getDirection())) {
                        return ResponseEntity.badRequest().body("Ce service existe déjà dans une autre direction");
                    }
                    break;

                case "ENCADREUR":
                    User chefService = userRepository.findById(createDTO.getChefServiceId())
                            .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));
                    if (!"CHEF_SERVICE".equals(chefService.getRole())) {
                        return ResponseEntity.badRequest().body("L'ID fourni ne correspond pas à un chef de service");
                    }
                    break;

                case "STAGIAIRE":
                    return ResponseEntity.badRequest().body("Utilisez /register/stagiaire pour les stagiaires");

                default:
                    return ResponseEntity.badRequest().body("Rôle non valide");
            }

            // Création de l'utilisateur
            User user = new User();
            user.setCivilite(createDTO.getCivilite());
            user.setNom(createDTO.getNom());
            user.setPrenom(createDTO.getPrenom());
            user.setEmail(createDTO.getEmail());
            user.setPassword(passwordEncoder.encode(createDTO.getPassword()));
            user.setRole(createDTO.getRole());
            user.setTelephone(createDTO.getTelephone());

            // Champs spécifiques selon le rôle
            if (createDTO.getDirection() != null) {
                user.setDirection(createDTO.getDirection());
            }
            if (createDTO.getService() != null) {
                user.setService(createDTO.getService());
            }
            if (createDTO.getChefServiceId() != null) {
                user.setChefServiceId(createDTO.getChefServiceId());

                // Pour les encadreurs, on récupère les infos du chef de service
                if ("ENCADREUR".equals(createDTO.getRole())) {
                    User chef = userRepository.findById(createDTO.getChefServiceId())
                            .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));
                    user.setDirection(chef.getDirection());
                    user.setService(chef.getService());
                }
            }

            userRepository.save(user);

            // Envoi d'email avec les identifiants
            emailService.sendAccountCreationEmail(
                    user.getEmail(),
                    "Votre compte a été créé",
                    createDTO.getEmail(),
                    createDTO.getPassword()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Compte créé avec succès",
                    "userId", user.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la création du compte", "error", e.getMessage()));
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


    // Connexion Secrétaire
        @PostMapping("/login/secretaire")
        public ResponseEntity<?> loginSecretaire(@RequestBody AuthDTO authDTO) {
            try {
                User user = userRepository.findByEmail(authDTO.getEmail())
                        .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

                if (!"SECRETAIRE".equals(user.getRole())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Accès réservé aux secrétaires");
                }

                if (!passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Email ou mot de passe incorrect");
                }

                String token = jwtTokenService.generateToken(user.getId());
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);

                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "user", user
                ));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(e.getMessage());
            }
        }

    // Connexion Chef de Service
    @PostMapping("/login/chef-service")
    public ResponseEntity<?> loginChefService(@RequestBody AuthDTO authDTO) {
        try {
            User user = userRepository.findByEmail(authDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

            if (!"CHEF_SERVICE".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Accès réservé aux chefs de service");
            }

            if (!passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Email ou mot de passe incorrect");
            }

            String token = jwtTokenService.generateToken(user.getId());
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", user,
                    "service", user.getService() // Retourne le service dans la réponse
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        }
    }

       // Connexion Encadreur
    @PostMapping("/login/encadreur")
    public ResponseEntity<?> loginEncadreur(@RequestBody AuthDTO authDTO) {
        try {
            User user = userRepository.findByEmail(authDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

            if (!"ENCADREUR".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Accès réservé aux encadreurs");
            }

            if (!passwordEncoder.matches(authDTO.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Email ou mot de passe incorrect");
            }

            String token = jwtTokenService.generateToken(user.getId());
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        }
    }

}



