package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import gestion.pac.gestionstagiairesbackend.service.NotificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/secretaire")
@CrossOrigin(origins = "http://localhost:3000")
public class SecretaireController {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final NotificationService notificationService;

    public SecretaireController(UserRepository userRepository,
                                JwtTokenService jwtTokenService,
                                NotificationService notificationService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.notificationService = notificationService;
    }


    // Endpoint pour récupérer les stagiaires affectés à la secrétaire connectée
    @GetMapping("/stagiaires")
    public ResponseEntity<?> getStagiairesAffectes(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification de l'authentification
            String token = authHeader.replace("Bearer ", "");
            Long secretaireId = jwtTokenService.validateAndGetUserId(token);
            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux secrétaires");
            }

            // Récupérer les stagiaires affectés à cette secrétaire
            List<User> stagiaires = userRepository.findByRoleAndSecretaireId("STAGIAIRE", secretaireId);

            // Transformer en DTO avec toutes les informations nécessaires
            List<Map<String, Object>> result = stagiaires.stream()
                    .map(s -> {
                        Map<String, Object> stagiaireMap = new HashMap<>();
                        stagiaireMap.put("id", s.getId());
                        stagiaireMap.put("nom", s.getNom());
                        stagiaireMap.put("prenom", s.getPrenom());
                        stagiaireMap.put("email", s.getEmail());
                        stagiaireMap.put("filiere", s.getFiliere());
                        stagiaireMap.put("dateDebut", s.getDateDebut());
                        stagiaireMap.put("dateFin", s.getDateFin());

                        // Ajout du chemin vers la note de service
                        String noteServicePath = "NOTE_SERVICE_" + s.getNom() + "_" + s.getPrenom() + ".pdf";
                        stagiaireMap.put("noteServicePath", noteServicePath);

                        return stagiaireMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des stagiaires: " + e.getMessage());
        }
    }

    @GetMapping("/stagiaires/{stagiaireId}/note-service")
    public ResponseEntity<?> getNoteDeService(
            @PathVariable Long stagiaireId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification de l'authentification
            String token = authHeader.replace("Bearer ", "");
            Long secretaireId = jwtTokenService.validateAndGetUserId(token);
            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux secrétaires");
            }

            // Vérifier que le stagiaire est bien affecté à cette secrétaire
            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (!secretaireId.equals(stagiaire.getSecretaireId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ce stagiaire ne vous est pas affecté");
            }

            // Chemin vers le fichier
            String fileName = "NOTE_SERVICE_" + stagiaire.getNom() + "_" + stagiaire.getPrenom() + ".pdf";
            Path filePath = Paths.get("generated-docs").resolve(fileName).normalize();

            // Vérifier que le fichier existe
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("La note de service n'a pas été trouvée");
            }

            // Lire le fichier
            byte[] fileContent = Files.readAllBytes(filePath);

            // Retourner le fichier
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération de la note de service: " + e.getMessage());
        }
    }
    // Endpoint pour récupérer les informations de la secrétaire connectée
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long secretaireId = jwtTokenService.validateAndGetUserId(token);
            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux secrétaires");
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", secretaire.getId());
            profile.put("civilite", secretaire.getCivilite());
            profile.put("nom", secretaire.getNom());
            profile.put("prenom", secretaire.getPrenom());
            profile.put("email", secretaire.getEmail());
            profile.put("telephone", secretaire.getTelephone());
            profile.put("direction", secretaire.getDirection());

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération du profil: " + e.getMessage());
        }
    }

    // Récupérer les chefs de service de ma direction avec leur service
    @GetMapping("/chefs-service")
    public ResponseEntity<?> getChefsServiceDeMaDirection(
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Authentification
            String token = authHeader.replace("Bearer ", "");
            Long secretaireId = jwtTokenService.validateAndGetUserId(token);
            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux secrétaires");
            }

            // Récupérer les chefs de service de la même direction
            List<User> chefsService = userRepository.findByRoleAndDirection(
                    "CHEF_SERVICE", secretaire.getDirection());

            // Transformation en DTO avec service
            List<Map<String, String>> result = chefsService.stream()
                    .map(cs -> Map.of(
                            "id", cs.getId().toString(),
                            "nomComplet", cs.getNom() + " " + cs.getPrenom(),
                            "email", cs.getEmail(),
                            "service", cs.getService() // Ajout du service
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Affecter un stagiaire à un chef de service
    @PostMapping("/affecter-chef-service")
    public ResponseEntity<?> affecterChefService(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Authentification
            String token = authHeader.replace("Bearer ", "");
            Long secretaireId = jwtTokenService.validateAndGetUserId(token);
            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux secrétaires");
            }

            // Validation des paramètres
            if (request == null || request.get("stagiaireId") == null || request.get("chefServiceId") == null) {
                return ResponseEntity.badRequest().body("Paramètres manquants");
            }

            Long stagiaireId = request.get("stagiaireId");
            Long chefServiceId = request.get("chefServiceId");

            // Vérification des entités
            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            User chefService = userRepository.findById(chefServiceId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Vérification que c'est bien un chef de service
            if (!"CHEF_SERVICE".equals(chefService.getRole())) {
                return ResponseEntity.badRequest().body("L'ID fourni ne correspond pas à un chef de service");
            }

            // Vérifications métier
            if (!secretaire.getId().equals(stagiaire.getSecretaireId())) {
                return ResponseEntity.badRequest().body("Ce stagiaire ne vous est pas affecté");
            }

            if (!chefService.getDirection().equals(secretaire.getDirection())) {
                return ResponseEntity.badRequest().body(
                        "Le chef de service n'est pas de votre direction. " +
                                "Direction chef: " + chefService.getDirection() +
                                " | Votre direction: " + secretaire.getDirection()
                );
            }

            // Vérification des affectations existantes
            if (stagiaire.getChefServiceId() != null) {
                if (stagiaire.getChefServiceId().equals(chefServiceId)) {
                    return ResponseEntity.badRequest().body("Ce stagiaire est déjà affecté à ce chef de service");
                }
                return ResponseEntity.badRequest().body(
                        "Ce stagiaire est déjà affecté à un autre chef de service. " +
                                "Annulez d'abord l'affectation existante"
                );
            }

            // Affectation
            stagiaire.setChefServiceId(chefServiceId);
            stagiaire.setStatut("AFFECTE_A_CHEF_SERVICE"); // Mise à jour du statut
            userRepository.save(stagiaire);

            // Notification
            notificationService.sendAffectationChefServiceNotification(chefServiceId, stagiaireId);

            return ResponseEntity.ok(Map.of(
                    "message", "Affectation réussie au chef de service " + chefService.getService(),
                    "stagiaireId", stagiaireId,
                    "chefServiceId", chefServiceId,
                    "service", chefService.getService()
            ));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur technique: " + e.getMessage());
        }
    }

    @PostMapping("/annuler-affectation-chef-service")
    public ResponseEntity<?> annulerAffectationChefService(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Authentification
            String token = authHeader.replace("Bearer ", "");
            Long secretaireId = jwtTokenService.validateAndGetUserId(token);
            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux secrétaires");
            }

            Long stagiaireId = request.get("stagiaireId");

            // Vérification des entités
            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            // Vérifications métier
            if (!secretaire.getId().equals(stagiaire.getSecretaireId())) {
                return ResponseEntity.badRequest().body("Ce stagiaire ne vous est pas affecté");
            }

            if (stagiaire.getChefServiceId() == null) {
                return ResponseEntity.badRequest().body("Ce stagiaire n'est pas affecté à un chef de service");
            }

            // Récupérer l'ancien chef de service pour notification
            Long ancienChefServiceId = stagiaire.getChefServiceId();
            User ancienChefService = userRepository.findById(ancienChefServiceId)
                    .orElseThrow(() -> new RuntimeException("Ancien chef de service non trouvé"));

            // Annulation de l'affectation
            stagiaire.setChefServiceId(null);
            stagiaire.setStatut("AFFECTE_A_SECRETAIRE");
            userRepository.save(stagiaire);

            // Envoyer une notification à l'ancien chef de service
            notificationService.sendAffectationChefServiceCancellationNotification(ancienChefServiceId, stagiaireId);

            return ResponseEntity.ok(Map.of(
                    "message", "Affectation au chef de service annulée avec succès",
                    "stagiaireId", stagiaireId,
                    "ancienChefServiceId", ancienChefServiceId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/chefs-service-avec-stagiaires")
    public ResponseEntity<?> getChefsServiceAvecStagiaires(
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Authentification
            String token = authHeader.replace("Bearer ", "");
            Long secretaireId = jwtTokenService.validateAndGetUserId(token);
            User secretaire = userRepository.findById(secretaireId)
                    .orElseThrow(() -> new RuntimeException("Secrétaire non trouvée"));

            if (!"SECRETAIRE".equals(secretaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux secrétaires");
            }

            // Récupérer les chefs de service de la même direction qui ont des stagiaires affectés
            List<User> chefsServiceAvecStagiaires = userRepository.findChefsServiceWithStagiaires(secretaire.getDirection());

            // Transformer en DTO avec les stagiaires affectés
            List<Map<String, Object>> result = chefsServiceAvecStagiaires.stream()
                    .map(chef -> {
                        // Récupérer les stagiaires affectés à ce chef de service
                        List<User> stagiaires = userRepository.findByRoleAndChefServiceId("STAGIAIRE", chef.getId());

                        // Transformer les stagiaires en DTO simple
                        List<Map<String, String>> stagiairesDTO = stagiaires.stream()
                                .map(s -> Map.of(
                                        "id", s.getId().toString(),
                                        "nomComplet", s.getNom() + " " + s.getPrenom(),
                                        "email", s.getEmail(),
                                        "filiere", s.getFiliere(),
                                        "dateDebut", s.getDateDebut().toString(),
                                        "dateFin", s.getDateFin().toString()
                                ))
                                .collect(Collectors.toList());

                        // Créer le DTO pour le chef de service
                        Map<String, Object> chefDTO = new HashMap<>();
                        chefDTO.put("id", chef.getId());
                        chefDTO.put("nomComplet", chef.getNom() + " " + chef.getPrenom());
                        chefDTO.put("email", chef.getEmail());
                        chefDTO.put("service", chef.getService());
                        chefDTO.put("telephone", chef.getTelephone());
                        chefDTO.put("nombreStagiaires", stagiaires.size());
                        chefDTO.put("stagiaires", stagiairesDTO);

                        return chefDTO;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des chefs de service avec stagiaires: " + e.getMessage());
        }
    }
}
