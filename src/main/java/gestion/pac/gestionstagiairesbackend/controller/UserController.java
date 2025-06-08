package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.UserDTO;
import gestion.pac.gestionstagiairesbackend.dto.UserResponseDTO;
import gestion.pac.gestionstagiairesbackend.entite.Direction;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.exception.DocumentValidationException;
import gestion.pac.gestionstagiairesbackend.repository.DirectionRepository;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.FileStorageService;
import gestion.pac.gestionstagiairesbackend.service.DocumentGenerationService;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import gestion.pac.gestionstagiairesbackend.service.GoogleCalendarService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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



    public UserController(UserRepository userRepository,
                          FileStorageService fileStorageService,
                          JwtTokenService jwtTokenService,
                          DocumentGenerationService documentGenerationService,
                          DirectionRepository directionRepository,
                          GoogleCalendarService calendarService
    ) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.jwtTokenService = jwtTokenService;
        this.documentGenerationService = documentGenerationService;
        this.directionRepository = directionRepository;
        this.calendarService = calendarService;


    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createUser(
            @ModelAttribute UserDTO userDTO,
            @RequestPart("cv") MultipartFile cv,
            @RequestPart("lettre") MultipartFile lettre,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Vérification de l'authentification
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenService.validateAndGetUserId(token);
            User connectedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!"STAGIAIRE".equals(connectedUser.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Seuls les stagiaires peuvent faire une demande");
            }

            // Vérification des places disponibles pour chaque direction sélectionnée
            for (String directionNom : userDTO.getDirections()) {
                Direction direction = directionRepository.findByNom(directionNom)
                        .orElseThrow(() -> new RuntimeException("Direction non trouvée"));

                if (direction.getPlacesOccupees() >= direction.getPlacesTotales()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Impossible de sélectionner " + directionNom +
                                    " - Plus de places disponibles"));
                }
            }

            // Mise à jour des informations utilisateur
            connectedUser.setCivilite(userDTO.getCivilite());
            connectedUser.setNom(userDTO.getNom());
            connectedUser.setPrenom(userDTO.getPrenom());
            connectedUser.setEmail(userDTO.getEmail());
            connectedUser.setContactUrgent(userDTO.getContactUrgent());
            connectedUser.setDirections(userDTO.getDirections());
            connectedUser.setConsentement(userDTO.getConsentement());
            connectedUser.setTypeStage(userDTO.getTypeStage());
            connectedUser.setNomEtablissement(userDTO.getNomEtablissement());
            connectedUser.setAdresseEtablissement(userDTO.getAdresseEtablissement());
            connectedUser.setMessage(userDTO.getMessage());
            connectedUser.setFiliere(userDTO.getFiliere());
            connectedUser.setAnneeAcademique(userDTO.getAnneeAcademique());
            connectedUser.setDateDebut(userDTO.getDateDebut());
            connectedUser.setDateFin(userDTO.getDateFin());
            connectedUser.setTelephone(userDTO.getTelephone());


            // Stockage des fichiers
            String cvPath = fileStorageService.storeFile(cv);
            String lettrePath = fileStorageService.storeFile(lettre);

            // Sauvegarde des chemins
            connectedUser.setCvPath(cvPath);
            connectedUser.setLettrePath(lettrePath);
            // Ajoutez la date de soumission
            connectedUser.setDateSoumission(LocalDateTime.now());
            User savedUser = userRepository.save(connectedUser);

            // Ajoutez l'événement au calendrier
            try {
                calendarService.addDemandeStageEvent(savedUser);
            } catch (Exception e) {
                // Log l'erreur mais ne bloquez pas la requête
                logger.error("Erreur lors de l'ajout au calendrier", e);
            }

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
                    new Direction("Direction des infrastructures", 2, 0),
                    new Direction("Direction commerciale et du marketing", 3, 0),
                    new Direction("Direction des ressources humaines", 4, 0),
                    new Direction("Direction des systèmes d'information", 6, 0),
                    new Direction("Direction des Operations Portuaires et de la sécurité", 6, 0),
                    new Direction("Direction du controle des marchés Publics", 6, 0),
                    new Direction("Direction de l'Administration et des Finances", 6, 0),
                    new Direction("Direction des Marchés Publics", 6, 0),
                    new Direction("Capitainerie du Port", 6, 0),
                    new Direction("Direction de l'Audit Interne et du Contrôle Financier", 6, 0),
                    new Direction("Département Qualité Santé Environnement", 6, 0),
                    new Direction("Direction des Affaires Juridiques et du Contentieux", 6, 0),
                    new Direction("Direction Générale", 6, 0)

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

        if (!"FICHE_ASSURANCE_VALIDEE".equals(user.getStatut())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("La fiche d'assurance doit être validée d'abord");
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

            if (!"FICHE_ASSURANCE_VALIDEE".equals(stagiaire.getStatut())) {
                return ResponseEntity.badRequest().body("Documents non disponibles");
            }

            // Mettre à jour le statut
            stagiaire.setStatut("DOCUMENT_COMPLET");
            userRepository.save(stagiaire);


            return ResponseEntity.ok(Map.of(
                    "message", "Documents confirmés téléchargés",
                    "statut", stagiaire.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
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