package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.AuthDTO;
import gestion.pac.gestionstagiairesbackend.dto.DemandeStageDTO;
import gestion.pac.gestionstagiairesbackend.dto.RHUserDTO;
import gestion.pac.gestionstagiairesbackend.dto.ValidationFicheDTO;
import gestion.pac.gestionstagiairesbackend.entite.Direction;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.DirectionRepository;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.EmailService;
import gestion.pac.gestionstagiairesbackend.service.FileStorageService;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final FileStorageService fileStorageService;




    @Value("${file.upload-dir}")
    private String uploadDir;


    public RHController(UserRepository userRepository, JwtTokenService jwtTokenService, EmailService emailService, GoogleCalendarService calendarService, DirectionRepository directionRepository,FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.emailService = emailService;
        this.calendarService = calendarService;
        this.directionRepository = directionRepository;
        this.fileStorageService = fileStorageService;


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



            userRepository.delete(demande);

            return ResponseEntity.ok(Map.of("message", "Demande supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


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
            // Vérifier les places disponibles avant validation
            if ("VALIDEE".equals(newStatus)) {
                for (String directionNom : demande.getDirections()) {
                    Direction direction = directionRepository.findByNom(directionNom)
                            .orElseThrow(() -> new RuntimeException("Direction non trouvée"));

                }
            }

            if ("REJETEE".equals(newStatus)) {
                // Supprimer directement l'utilisateur si rejeté
                userRepository.delete(demande);
            } else {
                // Sinon, mettre à jour le statut
                demande.setStatut(newStatus);
                userRepository.save(demande);

                if ("VALIDEE".equals(newStatus)) {
                    emailService.sendValidationEmail(
                            demande.getEmail(),
                            "Votre demande de stage a été acceptée"
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

    // Permet à un RH de récupérer la fiche d’assurance d’un stagiaire par son ID
    @GetMapping("/voir-fiche-assurance/{stagiaireId}")
    public ResponseEntity<?> voirFicheAssurance(
            @PathVariable Long stagiaireId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérifie le token JWT
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non autorisé");
            }

            User rh = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rh.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux RH");
            }

            // Récupère le stagiaire
            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            String fileName = stagiaire.getFicheAssurancePath();
            if (fileName == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fiche d’assurance non trouvée.");
            }

            Path filePath = fileStorageService.loadFileAsPath(fileName);
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fichier introuvable.");
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                    .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'affichage de la fiche : " + e.getMessage());
        }
    }

    @PutMapping("/valider-fiche-assurance/{stagiaireId}")
    public ResponseEntity<?> validerFicheAssurance(
            @PathVariable Long stagiaireId,
            @RequestBody ValidationFicheDTO validationDTO,
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

            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (!"EN_ATTENTE_VALIDATION".equals(stagiaire.getStatut())) {
                return ResponseEntity.badRequest().body("La fiche d'assurance n'est pas en attente de validation");
            }

            // Validation des dates
            if (validationDTO.isConfirmerPeriodeStagiaire()) {
                // On utilise les dates du stagiaire
                if (stagiaire.getDateDebut() == null || stagiaire.getDateFin() == null) {
                    return ResponseEntity.badRequest().body("Le stagiaire n'a pas défini de période de stage");
                }

                // Vérification que les dates du stagiaire sont valides
                if (stagiaire.getDateDebut().isBefore(LocalDate.now())) {
                    return ResponseEntity.badRequest().body("La date de début du stagiaire est dans le passé");
                }
                if (stagiaire.getDateFin().isBefore(stagiaire.getDateDebut())) {
                    return ResponseEntity.badRequest().body("La date de fin du stagiaire est avant la date de début");
                }
            } else {
                // On utilise les dates fournies par la RH
                if (validationDTO.getDateDebut() == null || validationDTO.getDateFin() == null) {
                    return ResponseEntity.badRequest().body("Vous devez définir une période de stage");
                }

                // Vérification des dates fournies par la RH
                if (validationDTO.getDateDebut().isBefore(LocalDate.now())) {
                    return ResponseEntity.badRequest().body("La date de début ne peut pas être dans le passé");
                }
                if (validationDTO.getDateFin().isBefore(validationDTO.getDateDebut())) {
                    return ResponseEntity.badRequest().body("La date de fin doit être après la date de début");
                }

                // Mise à jour des dates avec celles de la RH
                stagiaire.setDateDebut(validationDTO.getDateDebut());
                stagiaire.setDateFin(validationDTO.getDateFin());
            }

            // Mettre à jour le statut
            stagiaire.setStatut("FINALISE");
            stagiaire.setAlerte("Votre assurance est validée. Vous pouvez maintenant télécharger les documents pour commencer votre stage du " +
                    stagiaire.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " au " +
                    stagiaire.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // Sauvegarde
            userRepository.save(stagiaire);

            // Envoi d'email
            emailService.sendAssuranceValidationEmail(
                    stagiaire.getEmail(),
                    "Votre fiche d'assurance a été validée - Documents disponibles",
                    stagiaire.getDateDebut(),
                    stagiaire.getDateFin()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Fiche d'assurance validée et demande finalisée avec succès",
                    "statut", stagiaire.getStatut(),
                    "dateDebut", stagiaire.getDateDebut(),
                    "dateFin", stagiaire.getDateFin()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la validation: " + e.getMessage());
        }
    }
    @DeleteMapping("/rejeter-demande-assurance/{stagiaireId}")
    public ResponseEntity<?> rejeterEtSupprimerDemandeAssurance(
            @PathVariable Long stagiaireId,
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

            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (!"EN_ATTENTE_VALIDATION".equals(stagiaire.getStatut())) {
                return ResponseEntity.badRequest().body("La demande n'est pas en attente de validation");
            }

            // Envoyer l'email de notification
            emailService.sendRejetAssuranceEmail(
                    stagiaire.getEmail(),
                    "Votre fiche d'assurance a été rejetée"
            );

            // Supprimer la demande
            userRepository.delete(stagiaire);

            return ResponseEntity.ok(Map.of(
                    "message", "Demande rejetée et supprimée avec succès"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du rejet: " + e.getMessage());
        }
    }

    // Valider une pause
    @PutMapping("/valider-pause/{stagiaireId}")
    public ResponseEntity<?> validerPause(
            @PathVariable Long stagiaireId,
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

            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (stagiaire.getPauseStartDate() == null) {
                return ResponseEntity.badRequest().body("Aucune demande de pause en attente");
            }

            // Mise à jour du statut
            stagiaire.setStageStatus(User.StageStatus.PAUSED);
            stagiaire.setDateFin(stagiaire.getOriginalEndDate()); // Conserve la date originale
            stagiaire.setPauseRequestDate(null);

            userRepository.save(stagiaire);

            return ResponseEntity.ok(Map.of(
                    "message", "Pause validée avec succès",
                    "newStatus", "PAUSED"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Valider une reprise
    @PutMapping("/valider-reprise/{stagiaireId}")
    public ResponseEntity<?> validerReprise(
            @PathVariable Long stagiaireId,
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

            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (stagiaire.getPauseEndDate() == null) {
                return ResponseEntity.badRequest().body("Aucune demande de reprise en attente");
            }

            // Calcul de la nouvelle date de fin
            LocalDate newEndDate = stagiaire.getOriginalEndDate()
                    .plusDays(stagiaire.getRemainingDaysBeforePause());

            // Mise à jour du statut et des dates
            stagiaire.setStageStatus(User.StageStatus.ACTIVE);
            stagiaire.setDateFin(newEndDate);
            stagiaire.setPauseEndDate(null);
            stagiaire.setRepriseRequestDate(null);

            userRepository.save(stagiaire);

            return ResponseEntity.ok(Map.of(
                    "message", "Reprise validée avec succès",
                    "newStatus", "ACTIVE",
                    "newEndDate", newEndDate
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Archiver un stagiaire
    @PutMapping("/archiver/{stagiaireId}")
    public ResponseEntity<?> archiverStagiaire(
            @PathVariable Long stagiaireId,
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

            User stagiaire = userRepository.findById(stagiaireId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            if (stagiaire.getStageStatus() != User.StageStatus.COMPLETED) {
                return ResponseEntity.badRequest().body("Seuls les stagiaires terminés peuvent être archivés");
            }

            stagiaire.setStageStatus(User.StageStatus.ARCHIVED);
            userRepository.save(stagiaire);

            return ResponseEntity.ok(Map.of(
                    "message", "Stagiaire archivé avec succès",
                    "newStatus", "ARCHIVED"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Récupérer les demandes de pause/reprise
    @GetMapping("/demandes-pause-reprise")
    public ResponseEntity<?> getDemandesPauseReprise(
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

            // Récupère les stagiaires avec des demandes en attente
            List<User> demandesPause = userRepository.findByPauseRequestDateIsNotNull();
            List<User> demandesReprise = userRepository.findByRepriseRequestDateIsNotNull();

            // Combine les résultats
            List<Map<String, Object>> result = new ArrayList<>();

            demandesPause.forEach(s -> result.add(Map.of(
                    "type", "PAUSE",
                    "stagiaireId", s.getId(),
                    "nom", s.getNom(),
                    "prenom", s.getPrenom(),
                    "dateDebut", s.getPauseStartDate(),
                    "dateDemande", s.getPauseRequestDate()
            )));

            demandesReprise.forEach(s -> result.add(Map.of(
                    "type", "REPRISE",
                    "stagiaireId", s.getId(),
                    "nom", s.getNom(),
                    "prenom", s.getPrenom(),
                    "dateReprise", s.getPauseEndDate(),
                    "dateDemande", s.getRepriseRequestDate()
            )));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(@RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User rhUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"RH".equals(rhUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé à la RH");
            }

            // Statistiques
            long active = userRepository.countByStageStatus(User.StageStatus.ACTIVE);
            long paused = userRepository.countByStageStatus(User.StageStatus.PAUSED);
            long completed = userRepository.countByStageStatus(User.StageStatus.COMPLETED);
            long archived = userRepository.countByStageStatus(User.StageStatus.ARCHIVED);

            // Demandes en attente
            long pauseRequests = userRepository.countByPauseRequestDateIsNotNull();
            long repriseRequests = userRepository.countByRepriseRequestDateIsNotNull();

            // Stagiaires dont le stage se termine bientôt (dans les 7 jours)
            LocalDate today = LocalDate.now();
            List<User> endingSoon = userRepository.findByDateFinBetweenAndStageStatus(
                    today, today.plusDays(7), User.StageStatus.ACTIVE);

            return ResponseEntity.ok(Map.of(
                    "stats", Map.of(
                            "active", active,
                            "paused", paused,
                            "completed", completed,
                            "archived", archived
                    ),
                    "demandes", Map.of(
                            "pause", pauseRequests,
                            "reprise", repriseRequests
                    ),
                    "endingSoon", endingSoon.stream().map(s -> Map.of(
                            "id", s.getId(),
                            "nom", s.getNom(),
                            "prenom", s.getPrenom(),
                            "dateFin", s.getDateFin(),
                            "joursRestants", ChronoUnit.DAYS.between(today, s.getDateFin())
                    )).collect(Collectors.toList())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }
}