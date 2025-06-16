package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.DemandeAbsenceDTO;
import gestion.pac.gestionstagiairesbackend.dto.StageRequestDTO;
import gestion.pac.gestionstagiairesbackend.dto.UserDTO;
import gestion.pac.gestionstagiairesbackend.dto.UserResponseDTO;
import gestion.pac.gestionstagiairesbackend.entite.DemandeAbsence;
import gestion.pac.gestionstagiairesbackend.entite.Direction;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.DemandeAbsenceRepository;
import gestion.pac.gestionstagiairesbackend.repository.DirectionRepository;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.*;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final JwtTokenService jwtTokenService;
    private final DocumentGenerationService documentGenerationService; // <-- Ajoutez cette ligne
    private final DirectionRepository directionRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final GoogleCalendarService calendarService;
    private final DemandeAbsenceRepository demandeAbsenceRepository;
    private final EmailService emailService;



    public UserController(UserRepository userRepository,
                          FileStorageService fileStorageService,
                          JwtTokenService jwtTokenService,
                          DocumentGenerationService documentGenerationService,
                          DirectionRepository directionRepository,
                          GoogleCalendarService calendarService,
                          DemandeAbsenceRepository demandeAbsenceRepository,
                          EmailService emailService) {

        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.jwtTokenService = jwtTokenService;
        this.documentGenerationService = documentGenerationService;
        this.directionRepository = directionRepository;
        this.calendarService = calendarService;
        this.demandeAbsenceRepository = demandeAbsenceRepository;
        this.emailService = emailService;

    }

    @PostMapping(value = "/demande-stage", consumes = "multipart/form-data")
    public ResponseEntity<?> createDemandeStage(
            @ModelAttribute StageRequestDTO requestDTO,
            @RequestPart("cv") MultipartFile cv,
            @RequestPart("lettre") MultipartFile lettre,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User connectedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"STAGIAIRE".equals(connectedUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Seuls les stagiaires peuvent faire une demande");
            }

            // Suppression de la vérification de demande existante
            // (permet à un utilisateur de faire plusieurs demandes)

            // Mise à jour des informations utilisateur
            connectedUser.setCivilite(requestDTO.getCivilite());
            connectedUser.setNom(requestDTO.getNom());
            connectedUser.setPrenom(requestDTO.getPrenom());
            connectedUser.setEmail(requestDTO.getEmail());
            connectedUser.setContactUrgent(requestDTO.getContactUrgent());
            connectedUser.setDirections(requestDTO.getDirections());
            connectedUser.setConsentement(requestDTO.getConsentement());
            connectedUser.setTypeStage(requestDTO.getTypeStage());
            connectedUser.setNomEtablissement(requestDTO.getNomEtablissement());
            connectedUser.setAdresseEtablissement(requestDTO.getAdresseEtablissement());
            connectedUser.setMessage(requestDTO.getMessage());
            connectedUser.setFiliere(requestDTO.getFiliere());
            connectedUser.setAnneeAcademique(requestDTO.getAnneeAcademique());
            connectedUser.setDateDebut(requestDTO.getDateDebut());
            connectedUser.setDateFin(requestDTO.getDateFin());
            connectedUser.setTelephone(requestDTO.getTelephone());

            // Stockage des fichiers
            String cvPath = fileStorageService.storeFile(cv);
            String lettrePath = fileStorageService.storeFile(lettre);

            connectedUser.setCvPath(cvPath);
            connectedUser.setLettrePath(lettrePath);
            connectedUser.setDateSoumission(LocalDateTime.now());
            connectedUser.setStatut("EN_ATTENTE"); // Nouveau statut pour la demande

            User savedUser = userRepository.save(connectedUser);

            return ResponseEntity.ok(convertToResponseDTO(savedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/directions")
    public ResponseEntity<List<Direction>> getAllDirections() {
        return ResponseEntity.ok(directionRepository.findAll());
    }

    @PostConstruct
    public void initDirections() {
        if (directionRepository.count() == 0) {
            List<Direction> directions = List.of(
                    new Direction("Direction des infrastructures"),
                    new Direction("Direction commerciale et du marketing"),
                    new Direction("Direction des ressources humaines"),
                    new Direction("Direction des systèmes d'information"),
                    new Direction("Direction des Operations Portuaires et de la sécurité"),
                    new Direction("Direction du controle des marchés Publics"),
                    new Direction("Direction de l'Administration et des Finances"),
                    new Direction("Direction des Marchés Publics"),
                    new Direction("Capitainerie du Port"),
                    new Direction("Direction de l'Audit Interne et du Contrôle Financier"),
                    new Direction("Département Qualité Santé Environnement"),
                    new Direction("Direction des Affaires Juridiques et du Contentieux"),
                    new Direction("Direction Générale")
            );
            directionRepository.saveAll(directions);
        }
    }
    private UserResponseDTO convertToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setCivilite(user.getCivilite());
        dto.setNom(user.getNom());
        dto.setPrenom(user.getPrenom());
        dto.setEmail(user.getEmail());
        dto.setContactUrgent(user.getContactUrgent());
        dto.setDirections(user.getDirections());
        dto.setCvPath(user.getCvPath());
        dto.setLettrePath(user.getLettrePath());
        dto.setConsentement(user.getConsentement());
        dto.setTypeStage(user.getTypeStage());
        dto.setNomEtablissement(user.getNomEtablissement());
        dto.setAdresseEtablissement(user.getAdresseEtablissement());
        dto.setMessage(user.getMessage());
        dto.setFiliere(user.getFiliere());
        dto.setAnneeAcademique(user.getAnneeAcademique());
        dto.setDateDebut(user.getDateDebut());
        dto.setDateFin(user.getDateFin());
        dto.setFicheAssurancePath(user.getFicheAssurancePath());
        dto.setTelephone(user.getTelephone());


        return dto;
    }


    // Modifier l'endpoint d'upload pour ne pas mettre directement DOCUMENT_COMPLET
    @PutMapping("/{id}/upload-assurance")
    public ResponseEntity<?> uploadFicheAssurance(
            @PathVariable Long id,
            @RequestParam("assurance") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Validation du token
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Vérifications
            if (!userId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!"STAGIAIRE".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!"VALIDEE".equals(user.getStatut())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Le statut doit être VALIDEE");
            }

            // Sauvegarde du fichier
            String ficheAssurancePath = fileStorageService.storeFile(file);
            user.setFicheAssurancePath(ficheAssurancePath);
            user.setStatut("EN_ATTENTE_VALIDATION"); // Nouveau statut

            User updatedUser = userRepository.save(user);



            return ResponseEntity.ok(convertToResponseDTO(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Permet à un stagiaire connecté, avec le statut VALIDEE, d’uploader sa fiche d’assurance pour finaliser sa demande.
    @PutMapping("/finaliser-demande")
    public ResponseEntity<?> finaliserDemandeAvecFiche(
            @RequestParam("assurance") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Extraction et validation du token JWT
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilisateur non authentifié"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Vérifie que l'utilisateur est un stagiaire avec statut VALIDEE
            if (!"STAGIAIRE".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès réservé aux stagiaires"));
            }

            if (!"VALIDEE".equals(user.getStatut())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "La demande doit être au statut VALIDEE pour finaliser"));
            }

            // Enregistrer le fichier de fiche d’assurance
            String assurancePath = fileStorageService.storeFile(file);
            user.setFicheAssurancePath(assurancePath);

            // Mettre à jour le statut
            user.setStatut("EN_ATTENTE_VALIDATION");

            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Fiche d’assurance envoyée avec succès. Votre demande est en attente de validation.",
                    "statut", user.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l’envoi de la fiche : " + e.getMessage()));
        }
    }

    @GetMapping("/alerte")
    public ResponseEntity<?> getAlerte(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            return ResponseEntity.ok(Map.of(
                    "alerte", user.getAlerte(),
                    "statut", user.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération de l’alerte : " + e.getMessage());
        }
    }

    @GetMapping("/documents/{userId}/generate")
    public ResponseEntity<?> generateDocuments(@PathVariable Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur introuvable");
        }

        User user = userOptional.get();

        if (!"FINALISE".equals(user.getStatut())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("La demande doit être finalisée pour générer les documents");
        }

        try {
            // Génère les documents et récupère seulement les noms de fichiers
            String noteDeServicePath = documentGenerationService.generateNoteDeService(user);
            String demandeStagePath = documentGenerationService.generateDemandeStage(user);

            // Extraire juste le nom du fichier du chemin complet
            String noteDeServiceFilename = Paths.get(noteDeServicePath).getFileName().toString();
            String demandeStageFilename = Paths.get(demandeStagePath).getFileName().toString();

            Map<String, String> result = new HashMap<>();
            result.put("noteDeService", noteDeServiceFilename);
            result.put("demandeStage", demandeStageFilename);

            // Après génération des documents
            user.setNoteServicePath(noteDeServiceFilename);
            user.setDemandeStagePath(demandeStageFilename);
            userRepository.save(user);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la génération des documents : " + e.getMessage());
        }
    }

    @GetMapping("/documents/download/{filename:.+}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable String filename,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Vérification du token
            String token = authHeader.replace("Bearer ", "");
            jwtTokenService.validateAndGetUserId(token); // Lance une exception si invalide

            // Sécurité : vérification du path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            // Chemin base pour les documents générés
            Path documentsDir = Paths.get("generated-docs").toAbsolutePath().normalize();
            Path filePath = documentsDir.resolve(filename).normalize();

            // Vérification que le chemin est bien dans le dossier autorisé
            if (!filePath.startsWith(documentsDir)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/documents/{userId}/confirm")
    public ResponseEntity<?> confirmDocumentsDownloaded(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long currentUserId = jwtTokenService.validateAndGetUserId(token);
            User user = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!user.getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            User stagiaire = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Stagiaire non trouvé"));

            // Vérification simplifiée pour le statut FINALISE
            if (!"FINALISE".equals(stagiaire.getStatut())) {
                return ResponseEntity.badRequest().body("Documents non disponibles");
            }

            // Ne pas changer le statut, il reste FINALISE
            return ResponseEntity.ok(Map.of(
                    "message", "Documents téléchargés avec succès",
                    "statut", stagiaire.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Demande de pause
    @PostMapping("/demande-pause")
    public ResponseEntity<?> demandePause(
            @RequestBody StageRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User stagiaire = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"STAGIAIRE".equals(stagiaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux stagiaires");
            }

            if (stagiaire.getStageStatus() != User.StageStatus.ACTIVE) {
                return ResponseEntity.badRequest().body("Vous ne pouvez pas faire de demande de pause dans votre statut actuel");
            }

            // Calcul des jours restants avant la pause
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), stagiaire.getDateFin());
            stagiaire.setRemainingDaysBeforePause((int) joursRestants);
            stagiaire.setPauseRequestDate(LocalDate.now());
            stagiaire.setPauseStartDate(request.getStartDate());
            stagiaire.setOriginalEndDate(stagiaire.getDateFin()); // Sauvegarde la date de fin originale

            userRepository.save(stagiaire);

            return ResponseEntity.ok(Map.of(
                    "message", "Demande de pause envoyée à la RH",
                    "status", "EN_ATTENTE_VALIDATION_PAUSE"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Demande de reprise
    @PostMapping("/demande-reprise")
    public ResponseEntity<?> demandeReprise(
            @RequestBody StageRequestDTO request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User stagiaire = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"STAGIAIRE".equals(stagiaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux stagiaires");
            }

            if (stagiaire.getStageStatus() != User.StageStatus.PAUSED) {
                return ResponseEntity.badRequest().body("Vous ne pouvez pas faire de demande de reprise dans votre statut actuel");
            }

            stagiaire.setRepriseRequestDate(LocalDate.now());
            stagiaire.setPauseEndDate(request.getEndDate()); // Date proposée pour la reprise

            userRepository.save(stagiaire);

            return ResponseEntity.ok(Map.of(
                    "message", "Demande de reprise envoyée à la RH",
                    "status", "EN_ATTENTE_VALIDATION_REPRISE"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Endpoint pour créer une demande d'absence
    @PostMapping("/demande-absence")
    public ResponseEntity<?> creerDemandeAbsence(
            @RequestBody DemandeAbsenceDTO demandeDTO,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Vérification auth
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User stagiaire = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"STAGIAIRE".equals(stagiaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux stagiaires");
            }

            User encadreur = userRepository.findById(demandeDTO.getEncadreurId())
                    .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));

            if (!"ENCADREUR".equals(encadreur.getRole())) {
                return ResponseEntity.badRequest().body("L'ID fourni ne correspond pas à un encadreur");
            }

            // Vérifier que l'encadreur est bien celui du stagiaire
            if (!encadreur.getId().equals(stagiaire.getEncadreurId())) {
                return ResponseEntity.badRequest().body("Cet encadreur ne supervise pas ce stagiaire");
            }

            DemandeAbsence demande = new DemandeAbsence();
            demande.setStagiaire(stagiaire);
            demande.setEncadreur(encadreur);
            demande.setDateDebut(demandeDTO.getDateDebut());
            demande.setDateFin(demandeDTO.getDateFin());
            demande.setMotif(demandeDTO.getMotif());
            demande.setStatut("EN_ATTENTE");
            demande.setDateCreation(LocalDateTime.now());

            demandeAbsenceRepository.save(demande);

            // Après avoir sauvegardé la demande
            emailService.sendNotificationDemandeAbsence(encadreur, stagiaire, demande);

            return ResponseEntity.ok(Map.of(
                    "message", "Demande d'absence envoyée avec succès",
                    "demandeId", demande.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Endpoint pour récupérer les demandes d'un stagiaire
    @GetMapping("/mes-demandes-absence")
    public ResponseEntity<?> getMesDemandesAbsence(
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Authentification
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User stagiaire = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"STAGIAIRE".equals(stagiaire.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès réservé aux stagiaires"));
            }

            // Récupération des demandes
            List<DemandeAbsence> demandes = demandeAbsenceRepository.findByStagiaire(stagiaire);

            // Construction de la réponse
            List<Map<String, Object>> response = demandes.stream()
                    .map(d -> {
                        Map<String, Object> demandeMap = new HashMap<>();
                        demandeMap.put("id", d.getId());
                        demandeMap.put("dateDebut", d.getDateDebut());
                        demandeMap.put("dateFin", d.getDateFin());
                        demandeMap.put("motif", d.getMotif());
                        demandeMap.put("statut", d.getStatut());
                        demandeMap.put("commentaire", d.getCommentaire() != null ? d.getCommentaire() : "");
                        demandeMap.put("dateCreation", d.getDateCreation());
                        demandeMap.put("dateReponse", d.getDateReponse() != null ? d.getDateReponse() : "");
                        return demandeMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token invalide"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur dans getMesDemandesAbsence", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }



















    //Recupere simplement les infos de tous les utilisateurs connectés quelque soit le role ou le statut
    @GetMapping("/validate-token")
    public ResponseEntity<UserResponseDTO> getUserByToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            return ResponseEntity.ok(convertToResponseDTO(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}