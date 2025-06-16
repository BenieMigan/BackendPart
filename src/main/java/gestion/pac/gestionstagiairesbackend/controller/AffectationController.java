package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import gestion.pac.gestionstagiairesbackend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rh/affectations")
@CrossOrigin(origins = "http://localhost:3000")
public class AffectationController {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final NotificationService notificationService;

    public AffectationController(UserRepository userRepository,
                                 JwtTokenService jwtTokenService,
                                 NotificationService notificationService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.notificationService = notificationService;
    }
    // Lister toutes les secrétaires
    @GetMapping("/secretaires")
    public ResponseEntity<?> getAllSecretaires(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth RH
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<User> secretaires = userRepository.findByRole("SECRETAIRE");

            // Convertir en DTO
            List<Map<String, String>> result = secretaires.stream()
                    .map(s -> Map.of(
                            "id", s.getId().toString(),
                            "nom", s.getNom(),
                            "prenom", s.getPrenom(),
                            "email", s.getEmail(),
                            "direction", s.getDirection(),
                            "telephone", s.getTelephone()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Lister les stagiaires avec DOCUMENT_COMPLET
    @GetMapping("/stagiaires")
    public ResponseEntity<?> getStagiairesComplets(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth RH
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Utilisation de la nouvelle méthode
            List<User> stagiaires = userRepository.findByRoleAndStatut("STAGIAIRE", "FINALISE");

            // Convertir en DTO
            List<Map<String, Object>> result = stagiaires.stream()
                    .map(s -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", s.getId());
                        map.put("nom", s.getNom());
                        map.put("prenom", s.getPrenom());
                        map.put("email", s.getEmail());
                        map.put("directions", s.getDirections());
                        map.put("cvPath", s.getCvPath());
                        map.put("lettrePath", s.getLettrePath());
                        map.put("filiere", s.getFiliere());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
    @PostMapping("/affecter-secretaire")
    public ResponseEntity<?> affecterSecretaire(
            @RequestBody Map<String, Long> request,
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

            // Validation des paramètres
            if (request == null || request.get("stagiaireId") == null || request.get("secretaireId") == null) {
                return ResponseEntity.badRequest().body("Paramètres manquants");
            }

            Long stagiaireId = request.get("stagiaireId");
            Long secretaireId = request.get("secretaireId");

            // Récupération des entités
            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            // Vérification que c'est bien une secrétaire
            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.badRequest().body("L'ID fourni ne correspond pas à une secrétaire");
            }

            // Vérification du statut du stagiaire
            if (!"FINALISE".equals(stagiaire.getStatut())) {
                return ResponseEntity.badRequest().body("Le stagiaire n'a pas finalisé ses documents");
            }

            // Vérification des affectations existantes
            if (stagiaire.getSecretaireId() != null) {
                if (stagiaire.getSecretaireId().equals(secretaireId)) {
                    return ResponseEntity.badRequest().body("Ce stagiaire est déjà affecté à cette secrétaire");
                }
                return ResponseEntity.badRequest().body(
                        "Ce stagiaire est déjà affecté à une autre secrétaire. " +
                                "Vous devez d'abord annuler l'affectation existante"
                );
            }

            // Affectation (sans vérification de direction)
            stagiaire.setSecretaireId(secretaireId);
            stagiaire.setStatut("AFFECTE_A_SECRETAIRE"); // Mise à jour du statut
            userRepository.save(stagiaire);

            // Notification
            notificationService.sendAffectationNotification(secretaireId, stagiaireId);

            return ResponseEntity.ok(Map.of(
                    "message", "Affectation réussie",
                    "stagiaireId", stagiaireId,
                    "secretaireId", secretaireId,
                    "direction", secretaire.getDirection()
            ));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur technique: " + e.getMessage());
        }
    }
    @PostMapping("/annuler-affectation")
    public ResponseEntity<?> annulerAffectation(
            @RequestBody Map<String, Long> request,
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

            Long stagiaireId = request.get("stagiaireId");

            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            // Vérifier si le stagiaire est bien affecté à une secrétaire
            if (stagiaire.getSecretaireId() == null) {
                return ResponseEntity.badRequest().body("Ce stagiaire n'est pas actuellement affecté à une secrétaire");
            }

            // Récupérer l'ancienne secrétaire pour lui envoyer un message
            Long ancienneSecretaireId = stagiaire.getSecretaireId();
            User ancienneSecretaire = userRepository.findById(ancienneSecretaireId)
                    .orElseThrow(() -> new RuntimeException("Ancienne secrétaire non trouvée"));

            stagiaire.setSecretaireId(null);
            stagiaire.setStatut("FINALISE"); // Changé de "DOCUMENT_COMPLET" à "FINALISE"
            userRepository.save(stagiaire);

            // Envoyer un message d'excuse à l'ancienne secrétaire
            notificationService.sendAffectationCancellationNotification(ancienneSecretaireId, stagiaireId);

            return ResponseEntity.ok(Map.of(
                    "message", "Affectation annulée avec succès",
                    "stagiaireId", stagiaireId,
                    "ancienneSecretaireId", ancienneSecretaireId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
    // Detail d'une secretaire dans une direction spécifique
    @GetMapping("/secretaires/{direction}")
    public ResponseEntity<?> getSecretairesByDirection(
            @PathVariable String direction,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<User> secretaires = userRepository.findByRoleAndDirection("SECRETAIRE", direction);

            return ResponseEntity.ok(secretaires.stream()
                    .map(s -> Map.of(
                            "id", s.getId(),
                            "nomComplet", s.getNom() + " " + s.getPrenom(),
                            "email", s.getEmail()
                    ))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }


    @GetMapping("/secretaires/avec-stagiaires")
    public ResponseEntity<?> getSecretairesAvecStagiaires(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<User> secretaires = userRepository.findSecretairesWithStagiaires();

            List<Map<String, Object>> result = secretaires.stream().map(secretaire -> {
                List<User> stagiaires = userRepository.findByRoleAndSecretaireId("STAGIAIRE", secretaire.getId());

                List<Map<String, Object>> stagiaireInfos = stagiaires.stream().map(s -> {
                    Map<String, Object> stagiaireMap = new HashMap<>();
                    stagiaireMap.put("id", s.getId());
                    stagiaireMap.put("nom", s.getNom());
                    stagiaireMap.put("prenom", s.getPrenom());
                    stagiaireMap.put("email", s.getEmail());
                    stagiaireMap.put("filiere", s.getFiliere());
                    stagiaireMap.put("dateDebut", s.getDateDebut());
                    stagiaireMap.put("dateFin", s.getDateFin());
                    return stagiaireMap;
                }).collect(Collectors.toList());

                Map<String, Object> secretaireMap = new HashMap<>();
                secretaireMap.put("secretaireId", secretaire.getId());
                secretaireMap.put("nom", secretaire.getNom());
                secretaireMap.put("prenom", secretaire.getPrenom());
                secretaireMap.put("email", secretaire.getEmail());
                secretaireMap.put("direction", secretaire.getDirection()); // Ajout de la direction
                secretaireMap.put("nombreStagiaires", stagiaires.size()); // Ajout du nombre de stagiaires
                secretaireMap.put("stagiaires", stagiaireInfos);
                return secretaireMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
    @GetMapping("/chefs-service")
    public ResponseEntity<?> getChefsService(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<User> chefs = userRepository.findByRole("CHEF_SERVICE");
            long total = chefs.size();

            List<Map<String, String>> chefsDTO = chefs.stream()
                    .map(c -> Map.of(
                            "id", c.getId().toString(),
                            "nom", c.getNom(),
                            "prenom", c.getPrenom(),
                            "email", c.getEmail(),
                            "direction", c.getDirection()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "total", total,
                    "chefsService", chefsDTO
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

}