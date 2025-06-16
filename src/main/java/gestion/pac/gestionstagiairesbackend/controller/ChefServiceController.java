package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import gestion.pac.gestionstagiairesbackend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chef-service")
@CrossOrigin(origins = "http://localhost:3000")
public class ChefServiceController {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final NotificationService notificationService;

    public ChefServiceController(UserRepository userRepository,
                                 JwtTokenService jwtTokenService,
                                 NotificationService notificationService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.notificationService = notificationService;
    }

    // Récupérer les encadreurs sous ma supervision
    @GetMapping("/encadreurs")
    public ResponseEntity<?> getEncadreurs(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long chefServiceId = jwtTokenService.validateAndGetUserId(token);
            User chefService = userRepository.findById(chefServiceId)
                    .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));

            if (!"CHEF_SERVICE".equals(chefService.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux chefs de service");
            }

            List<User> encadreurs = userRepository.findByRoleAndChefServiceId("ENCADREUR", chefServiceId);

            List<Map<String, Object>> result = encadreurs.stream()
                    .map(e -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", e.getId());
                        map.put("nomComplet", e.getNom() + " " + e.getPrenom());
                        map.put("email", e.getEmail());
                        map.put("telephone", e.getTelephone());
                        map.put("specialite", e.getService());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
    // Récupérer les stagiaires affectés à mon service
    @GetMapping("/stagiaires")
    public ResponseEntity<?> getStagiaires(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long chefServiceId = jwtTokenService.validateAndGetUserId(token);
            User chefService = userRepository.findById(chefServiceId)
                    .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));

            if (!"CHEF_SERVICE".equals(chefService.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux chefs de service");
            }

            List<User> stagiaires = userRepository.findByRoleAndChefServiceId("STAGIAIRE", chefServiceId);

            List<Map<String, Object>> result = stagiaires.stream()
                    .map(s -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", s.getId());
                        map.put("nomComplet", s.getNom() + " " + s.getPrenom());
                        map.put("email", s.getEmail());
                        map.put("filiere", s.getFiliere());
                        map.put("dateDebut", s.getDateDebut());
                        map.put("dateFin", s.getDateFin());
                        map.put("encadreurId", s.getEncadreurId());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Affecter un stagiaire à un encadreur
    @PostMapping("/affecter-encadreur")
    public ResponseEntity<?> affecterEncadreur(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long chefServiceId = jwtTokenService.validateAndGetUserId(token);
            User chefService = userRepository.findById(chefServiceId)
                    .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));

            if (!"CHEF_SERVICE".equals(chefService.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux chefs de service");
            }

            // Validation des paramètres
            if (request == null || request.get("stagiaireId") == null || request.get("encadreurId") == null) {
                return ResponseEntity.badRequest().body("Paramètres manquants");
            }

            Long stagiaireId = request.get("stagiaireId");
            Long encadreurId = request.get("encadreurId");

            // Vérification des entités
            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            User encadreur = userRepository.findById(encadreurId)
                    .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));

            // Vérifications métier
            if (!chefServiceId.equals(stagiaire.getChefServiceId())) {
                return ResponseEntity.badRequest().body("Ce stagiaire ne fait pas partie de votre service");
            }

            if (!encadreur.getChefServiceId().equals(chefServiceId)) {
                return ResponseEntity.badRequest().body("Cet encadreur ne fait pas partie de votre service");
            }

            // Affectation
            stagiaire.setEncadreurId(encadreurId);
            stagiaire.setStatut("AFFECTE_A_ENCADREUR");
            userRepository.save(stagiaire);

            // Notification
            notificationService.sendAffectationEncadreurNotification(encadreurId, stagiaireId);

            return ResponseEntity.ok(Map.of(
                    "message", "Affectation réussie à l'encadreur",
                    "stagiaireId", stagiaireId,
                    "encadreurId", encadreurId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Annuler l'affectation à un encadreur
    @PostMapping("/annuler-affectation-encadreur")
    public ResponseEntity<?> annulerAffectationEncadreur(
            @RequestBody Map<String, Long> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long chefServiceId = jwtTokenService.validateAndGetUserId(token);
            User chefService = userRepository.findById(chefServiceId)
                    .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));

            if (!"CHEF_SERVICE".equals(chefService.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux chefs de service");
            }

            Long stagiaireId = request.get("stagiaireId");

            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (!chefServiceId.equals(stagiaire.getChefServiceId())) {
                return ResponseEntity.badRequest().body("Ce stagiaire ne fait pas partie de votre service");
            }

            if (stagiaire.getEncadreurId() == null) {
                return ResponseEntity.badRequest().body("Ce stagiaire n'est pas affecté à un encadreur");
            }

            Long ancienEncadreurId = stagiaire.getEncadreurId();
            stagiaire.setEncadreurId(null);
            stagiaire.setStatut("AFFECTE_A_CHEF_SERVICE");
            userRepository.save(stagiaire);

            // Notification
            notificationService.sendAffectationEncadreurCancellationNotification(ancienEncadreurId, stagiaireId);

            return ResponseEntity.ok(Map.of(
                    "message", "Affectation à l'encadreur annulée",
                    "stagiaireId", stagiaireId,
                    "ancienEncadreurId", ancienEncadreurId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/encadreurs-avec-stagiaires")
    public ResponseEntity<?> getEncadreursAvecStagiaires(
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Authentification
            String token = authHeader.replace("Bearer ", "");
            Long chefServiceId = jwtTokenService.validateAndGetUserId(token);
            User chefService = userRepository.findById(chefServiceId)
                    .orElseThrow(() -> new RuntimeException("Chef de service non trouvé"));

            if (!"CHEF_SERVICE".equals(chefService.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux chefs de service");
            }

            // Récupérer les encadreurs du service qui ont des stagiaires affectés
            List<User> encadreursAvecStagiaires = userRepository.findByRoleAndChefServiceId("ENCADREUR", chefServiceId)
                    .stream()
                    .filter(e -> !userRepository.findByRoleAndEncadreurId("STAGIAIRE", e.getId()).isEmpty())
                    .collect(Collectors.toList());

            // Transformer en DTO avec les stagiaires affectés
            List<Map<String, Object>> result = encadreursAvecStagiaires.stream()
                    .map(encadreur -> {
                        // Récupérer les stagiaires affectés à cet encadreur
                        List<User> stagiaires = userRepository.findByRoleAndEncadreurId("STAGIAIRE", encadreur.getId());

                        // Transformer les stagiaires en DTO simple
                        List<Map<String, String>> stagiairesDTO = stagiaires.stream()
                                .map(s -> {
                                    Map<String, String> stagiaireMap = new HashMap<>();
                                    stagiaireMap.put("id", s.getId().toString());
                                    stagiaireMap.put("nomComplet", s.getNom() + " " + s.getPrenom());
                                    stagiaireMap.put("email", s.getEmail());
                                    stagiaireMap.put("filiere", s.getFiliere());
                                    stagiaireMap.put("dateDebut", s.getDateDebut().toString());
                                    stagiaireMap.put("dateFin", s.getDateFin().toString());
                                    return stagiaireMap;
                                })
                                .collect(Collectors.toList());

                        // Créer le DTO pour l'encadreur
                        Map<String, Object> encadreurDTO = new HashMap<>();
                        encadreurDTO.put("id", encadreur.getId());
                        encadreurDTO.put("nomComplet", encadreur.getNom() + " " + encadreur.getPrenom());
                        encadreurDTO.put("email", encadreur.getEmail());
                        encadreurDTO.put("telephone", encadreur.getTelephone());
                        encadreurDTO.put("specialite", encadreur.getService());
                        encadreurDTO.put("nombreStagiaires", stagiaires.size());
                        encadreurDTO.put("stagiaires", stagiairesDTO);

                        return encadreurDTO;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des encadreurs avec stagiaires: " + e.getMessage());
        }
    }
}