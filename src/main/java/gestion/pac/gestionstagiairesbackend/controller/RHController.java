package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.AuthDTO;
import gestion.pac.gestionstagiairesbackend.dto.DemandeStageDTO;
import gestion.pac.gestionstagiairesbackend.dto.RHUserDTO;
import gestion.pac.gestionstagiairesbackend.entite.Direction;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.DirectionRepository;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.EmailService;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import gestion.pac.gestionstagiairesbackend.service.GoogleCalendarService;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rh")
@CrossOrigin(origins = "http://localhost:3000")
public class RHController {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final EmailService emailService;
    private final GoogleCalendarService calendarService;
    private final DirectionRepository directionRepository;



    @Value("${file.upload-dir}")
    private String uploadDir;


    public RHController(UserRepository userRepository, JwtTokenService jwtTokenService, EmailService emailService, GoogleCalendarService calendarService,DirectionRepository directionRepository) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.emailService = emailService;
        this.calendarService = calendarService;
        this.directionRepository = directionRepository;

    }

    // Récupérer toutes les demandes de stage
    @GetMapping("/demandes")
    public ResponseEntity<?> getAllDemandes(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification du token et du rôle
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Récupération de toutes les demandes de stage (utilisateurs avec rôle STAGIAIRE)
            List<User> demandes = userRepository.findByRole("STAGIAIRE");

            // Conversion en DTO pour éviter d'exposer des données sensibles
            List<DemandeStageDTO> demandesDTO = demandes.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(demandesDTO);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erreur: " + e.getMessage());
        }
    }

    private DemandeStageDTO convertToDTO(User user) {
        DemandeStageDTO dto = new DemandeStageDTO();
        dto.setId(user.getId());
        dto.setCivilite(user.getCivilite());
        dto.setNom(user.getNom());
        dto.setPrenom(user.getPrenom());
        dto.setEmail(user.getEmail());
        dto.setContactUrgent(user.getContactUrgent());
        dto.setDirections(user.getDirections());
        dto.setTypeStage(user.getTypeStage());
        dto.setNomEtablissement(user.getNomEtablissement());
        dto.setAdresseEtablissement(user.getAdresseEtablissement());
        dto.setMessage(user.getMessage());
        dto.setStatut(user.getStatut());
        dto.setFiliere(user.getFiliere());
        dto.setAnneeAcademique(user.getAnneeAcademique());
        dto.setDateDebut(user.getDateDebut());
        dto.setDateFin(user.getDateFin());
        dto.setDateSoumission(user.getDateSoumission()); // Supposons que vous avez ce champ
        dto.setFicheAssurancePath(user.getFicheAssurancePath());
        dto.setTelephone(user.getTelephone()); // Ajoutez cette ligne
// Ajoutez cette ligne
        return dto;
    }

    // Endpoint pour supprimer une demande

    @DeleteMapping("/demandes/{id}")
    public ResponseEntity<?> deleteDemande(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            User demande = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            // Libérer les places si la demande était finalisée
            if ("DOCUMENT_COMPLET".equals(demande.getStatut())) {
                for (String directionNom : demande.getDirections()) {
                    Direction direction = directionRepository.findByNom(directionNom)
                            .orElseThrow(() -> new RuntimeException("Direction non trouvée"));
                    direction.setPlacesOccupees(direction.getPlacesOccupees() - 1);
                    directionRepository.save(direction);
                }
            }

            userRepository.delete(demande);

            return ResponseEntity.ok(Map.of("message", "Demande supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Modifiez la méthode updateDemandeStatus pour empêcher le rejet d'une demande validée

    @PutMapping("/demandes/{id}/status")
    public ResponseEntity<?> updateDemandeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Mise à jour du statut
            User demande = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            // Empêcher toute modification si le statut est DOCUMENT_COMPLET
            if ("DOCUMENT_COMPLET".equals(demande.getStatut())) {
                return ResponseEntity.badRequest().body("Impossible de modifier une demande déjà finalisée");
            }

            String newStatus = request.get("statut");

            // Empêcher le rejet d'une demande validée
            if ("REJETEE".equals(newStatus) && "VALIDEE".equals(demande.getStatut())) {
                return ResponseEntity.badRequest().body("Impossible de rejeter une demande déjà validée");
            }

            if ("REJETEE".equals(newStatus)) {
                // Supprimer directement l'utilisateur si rejeté
                userRepository.delete(demande);
            } else {
                // Sinon, mettre à jour le statut
                demande.setStatut(newStatus);
                userRepository.save(demande);

                // Envoi d'email si le statut est VALIDEE
                if ("VALIDEE".equals(newStatus)) {
                    String validationToken = jwtTokenService.generateToken(demande.getId());
                    String validationLink = "http://localhost:3000/finalisation/" + validationToken;
                    emailService.sendValidationEmail(
                            demande.getEmail(),
                            "Votre demande de stage a été acceptée",
                            validationLink
                    );
                    calendarService.addDemandeStageEvent(demande);
                }
            }

            return ResponseEntity.ok(Map.of("message", "Statut mis à jour"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/documents/{userId}/cv")
    public ResponseEntity<Resource> downloadCV(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        // Vérification de l'authentification RH
        String token = authHeader.replace("Bearer ", "");
        Long rhUserId = jwtTokenService.validateAndGetUserId(token);
        User rhUser = userRepository.findById(rhUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!"RH".equals(rhUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Récupération du stagiaire
        User stagiaire = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        if (stagiaire.getCvPath() == null) {
            return ResponseEntity.notFound().build();
        }

        // Construction du chemin complet
        Path filePath = Paths.get(uploadDir).resolve(stagiaire.getCvPath()).normalize();
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cv_" + stagiaire.getId() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/documents/{userId}/lettre")
    public ResponseEntity<Resource> downloadLettre(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        // Vérification de l'authentification RH
        String token = authHeader.replace("Bearer ", "");
        Long rhUserId = jwtTokenService.validateAndGetUserId(token);
        User rhUser = userRepository.findById(rhUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!"RH".equals(rhUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Récupération du stagiaire
        User stagiaire = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        if (stagiaire.getLettrePath() == null) {
            return ResponseEntity.notFound().build();
        }

        // Construction du chemin complet
        Path filePath = Paths.get(uploadDir).resolve(stagiaire.getLettrePath()).normalize();
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"lettre_" + stagiaire.getId() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/documents/{userId}/assurance")
    public ResponseEntity<Resource> downloadAssurance(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) throws IOException {

        String token = authHeader.replace("Bearer ", "");
        Long rhUserId = jwtTokenService.validateAndGetUserId(token);
        User rhUser = userRepository.findById(rhUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 🟢 AJOUTE CETTE VÉRIFICATION
        if (!"RH".equals(rhUser.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User stagiaire = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

        if (stagiaire.getFicheAssurancePath() == null) {
            return ResponseEntity.notFound().build();
        }

        // Exemple de mise à jour du statut à "DOCUMENT COMPLET" si c’est le comportement attendu
        if (!"DOCUMENT_COMPLET".equals(stagiaire.getStatut())) {
            stagiaire.setStatut("DOCUMENT_COMPLET");
            userRepository.save(stagiaire);
        }

        Path filePath = Paths.get(uploadDir).resolve(stagiaire.getFicheAssurancePath()).normalize();
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"assurance_" + stagiaire.getId() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader("Authorization") String authHeader) {
        try {

            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Statistiques globales
            long totalDemandes = userRepository.countByRole("STAGIAIRE");
            long demandesValidees = userRepository.countByStatut("VALIDEE");
            long dossiersFinalises = userRepository.countByStatut("DOCUMENT_COMPLET");

            // Stagiaires avec dossiers finalisés
            List<User> stagiairesFinalises = userRepository.findByStatut("DOCUMENT_COMPLET");

            // Statistiques par département
            List<Map<String, Object>> statsParDepartement = userRepository.getStatsByDepartment();


            // Notifications pour fins de stage proches (dans les 15 jours)
            LocalDate now = LocalDate.now();
            List<User> finsProches = userRepository.findByDateFinBetweenAndStatut(
                    now,
                    now.plusDays(15),
                    "DOCUMENT_COMPLET"
            );

            // Construction de la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("totalDemandes", totalDemandes);
            response.put("demandesValidees", demandesValidees);
            response.put("dossiersFinalises", dossiersFinalises);
            response.put("stagiairesFinalises", stagiairesFinalises.stream()
                    .map(this::convertToDashboardDTO)
                    .collect(Collectors.toList()));
            response.put("statsParDepartement", statsParDepartement);            response.put("finsProches", finsProches.stream()
                    .map(this::convertToNotificationDTO)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    private Map<String, Object> convertToDashboardDTO(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("nomComplet", user.getPrenom() + " " + user.getNom());

        // Utiliser la date de soumission comme date de début
        dto.put("dateDebut", user.getDateSoumission().toLocalDate()); // Convertir LocalDateTime en LocalDate

        // Calculer la date de fin (3 mois après la date de début)
        dto.put("dateFin", user.getDateSoumission().toLocalDate().plusMonths(3));

        dto.put("departements", user.getDirections());
        dto.put("email", user.getEmail());
        dto.put("telephone", user.getTelephone());
        return dto;
    }

    private Map<String, Object> convertToNotificationDTO(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("nomComplet", user.getPrenom() + " " + user.getNom());
        dto.put("dateFin", user.getDateFin());
        dto.put("joursRestants", ChronoUnit.DAYS.between(LocalDate.now(), user.getDateFin()));
        dto.put("departementPrincipal", user.getDirections().isEmpty() ? "" : user.getDirections().get(0));
        return dto;
    }

    // Ajoutez ces nouveaux endpoints
    @GetMapping("/dashboard/capacity")
    public ResponseEntity<?> getDepartmentCapacity(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<Map<String, Object>> capacityData = userRepository.getDepartmentCapacity();

            // Créer une nouvelle liste avec des nouvelles Maps modifiables
            List<Map<String, Object>> result = new ArrayList<>();

            for (Map<String, Object> dept : capacityData) {
                // Créer une nouvelle HashMap à partir des données de la requête
                Map<String, Object> newDept = new HashMap<>(dept);
                int total = ((Number) newDept.get("total")).intValue();
                int occupied = ((Number) newDept.get("occupied")).intValue();
                newDept.put("remaining", total - occupied);
                result.add(newDept);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/dashboard/quarter-stats")
    public ResponseEntity<?> getQuarterStats(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<Map<String, Object>> quarterStats = userRepository.getDemandesByQuarter();
            return ResponseEntity.ok(quarterStats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard/year-stats")
    public ResponseEntity<?> getYearStats(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<Map<String, Object>> yearStats = userRepository.getDemandesByYear();
            return ResponseEntity.ok(yearStats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard/demandes-by-period")
    public ResponseEntity<?> getDemandesByPeriod(
            @RequestParam int quarter,
            @RequestParam int year,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<User> demandes = userRepository.findDemandesByQuarterAndYear(quarter, year);
            List<DemandeStageDTO> demandesDTO = demandes.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(demandesDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Ajoutez ces endpoints dans RHController.java

    @GetMapping("/stagiaires-finalises/{year}")
    public ResponseEntity<?> getStagiairesFinalisesByYear(
            @PathVariable int year,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<User> stagiaires = userRepository.findByStatutAndYear("DOCUMENT_COMPLET", year);
            List<DemandeStageDTO> dtos = stagiaires.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stagiaires-finalises/{year}/{quarter}")
    public ResponseEntity<?> getStagiairesFinalisesByQuarter(
            @PathVariable int year,
            @PathVariable int quarter,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            List<User> stagiaires = userRepository.findByStatutAndQuarter("DOCUMENT_COMPLET", year, quarter);
            List<DemandeStageDTO> dtos = stagiaires.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    // Ajoutez ces nouvelles méthodes dans RHController.java

    @GetMapping("/dashboard/completed-by-year")
    public ResponseEntity<?> getCompletedByYear(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Requête pour les stagiaires complétés par année
            List<Map<String, Object>> stats = userRepository.getCompletedByYear();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard/completed-by-quarter")
    public ResponseEntity<?> getCompletedByQuarter(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Requête pour les stagiaires complétés par trimestre
            List<Map<String, Object>> stats = userRepository.getCompletedByQuarter();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/stagiaires-finalises/{id}")
    public ResponseEntity<?> deleteStagiaire(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            User stagiaire = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (!"DOCUMENT_COMPLET".equals(stagiaire.getStatut())) {
                return ResponseEntity.badRequest().body("Seuls les stagiaires avec dossier complet peuvent être supprimés");
            }

            // Libérer les places dans les directions
            for (String directionNom : stagiaire.getDirections()) {
                Direction direction = directionRepository.findByNom(directionNom)
                        .orElseThrow(() -> new RuntimeException("Direction non trouvée"));
                direction.setPlacesOccupees(direction.getPlacesOccupees() - 1);
                directionRepository.save(direction);
            }

            userRepository.delete(stagiaire);

            return ResponseEntity.ok(Map.of("message", "Stagiaire supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}