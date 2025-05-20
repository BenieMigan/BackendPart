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
import java.util.List;
import java.util.Map;

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

            // Vérification des places disponibles
            for (String directionNom : userDTO.getDirections()) {
                Direction direction = directionRepository.findByNom(directionNom)
                        .orElseThrow(() -> new RuntimeException("Direction non trouvée"));

                if (direction.getPlacesOccupees() >= direction.getPlacesTotales()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Impossible de sélectionner " + directionNom +
                                    " - Plus de places disponibles"));
                }
            }

            // Mise à jour des places occupées
            for (String directionNom : userDTO.getDirections()) {
                Direction direction = directionRepository.findByNom(directionNom).get();
                direction.setPlacesOccupees(direction.getPlacesOccupees() + 1);
                directionRepository.save(direction);
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
                    new Direction("Direction des infrastructures", 5, 0),
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

    @GetMapping("/validate-token")
    public ResponseEntity<UserResponseDTO> getUserByToken(@RequestParam String token) {
        Long userId = jwtTokenService.validateAndGetUserId(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ResponseEntity.ok(convertToResponseDTO(user));
    }

    @PutMapping("/{id}/upload-assurance")
    public ResponseEntity<?> uploadFicheAssurance(
            @PathVariable Long id,
            @RequestParam("assurance") MultipartFile file,
            @RequestParam String token) {

        try {
            // Validation du token
            Long userId = jwtTokenService.validateAndGetUserId(token);
            if (userId == null || !userId.equals(id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Vérification du statut
            if (!"VALIDEE".equals(user.getStatut())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Cette demande ne peut pas être finalisée"));
            }

            // Sauvegarde du fichier
            String ficheAssurancePath = fileStorageService.storeFile(file);
            user.setFicheAssurancePath(ficheAssurancePath);
            user.setStatut("DOCUMENT_COMPLET"); // Mise à jour du statut

            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(convertToResponseDTO(updatedUser));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du stockage du fichier"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/validate-finalization-token")
    public ResponseEntity<?> validateFinalizationToken(@RequestParam String token) {
        try {
            Long userId = jwtTokenService.validateAndGetUserId(token);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token invalide ou expiré"));
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Vérifiez que l'utilisateur est bien un stagiaire
            if (!"STAGIAIRE".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accès réservé aux stagiaires"));
            }

            // Vérifiez que le statut est VALIDEE
            if (!"VALIDEE".equals(user.getStatut())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Cette demande ne peut pas être finalisée"));
            }

            return ResponseEntity.ok(convertToResponseDTO(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/{id}/generate-note-service")
    public ResponseEntity<Resource> generateNoteService(
            @PathVariable Long id,
            @RequestParam String token) {
        try {
            // Validation
            Long userId = jwtTokenService.validateAndGetUserId(token);
            if (userId == null || !userId.equals(id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Vérification que la fiche d'assurance est uploadée
            if (user.getFicheAssurancePath() == null || user.getFicheAssurancePath().isEmpty()) {
                throw new DocumentValidationException("Vous devez d'abord uploader votre fiche d'assurance");
            }

            // Vérification que le statut est DOCUMENT_COMPLET
            if (!"DOCUMENT_COMPLET".equals(user.getStatut())) {
                throw new DocumentValidationException("La demande n'est pas encore complète");
            }

            // Génération
            String filePath = documentGenerationService.generateNoteDeService(user);
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("Fichier non généré");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"note-service.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (DocumentValidationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    e
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de génération: " + e.getMessage(),
                    e
            );
        }
    }

    @GetMapping("/{id}/generate-demande-stage")
    public ResponseEntity<Resource> generateDemandeStage(
            @PathVariable Long id,
            @RequestParam String token) {
        try {
            // Validation
            Long userId = jwtTokenService.validateAndGetUserId(token);
            if (userId == null || !userId.equals(id)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Vérification que la fiche d'assurance est uploadée
            if (user.getFicheAssurancePath() == null || user.getFicheAssurancePath().isEmpty()) {
                throw new DocumentValidationException("Vous devez d'abord uploader votre fiche d'assurance");
            }

            // Vérification que le statut est DOCUMENT_COMPLET
            if (!"DOCUMENT_COMPLET".equals(user.getStatut())) {
                throw new DocumentValidationException("La demande n'est pas encore complète");
            }

            // Génération
            String filePath = documentGenerationService.generateDemandeStage(user);
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("Fichier non généré");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"demande-stage.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (DocumentValidationException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    e
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de génération: " + e.getMessage(),
                    e
            );
        }
    }
}