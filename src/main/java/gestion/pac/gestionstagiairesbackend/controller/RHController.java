package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.AuthDTO;
import gestion.pac.gestionstagiairesbackend.dto.DemandeStageDTO;
import gestion.pac.gestionstagiairesbackend.dto.RHUserDTO;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.EmailService;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
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

    @Value("${file.upload-dir}")
    private String uploadDir;


    public RHController(UserRepository userRepository, JwtTokenService jwtTokenService, EmailService emailService, GoogleCalendarService calendarService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.emailService = emailService;
        this.calendarService = calendarService;

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

            String newStatus = request.get("statut");
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

                // Mettez à jour l'événement dans le calendrier
                calendarService.addDemandeStageEvent(demande);
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
        if (!"DOCUMENT COMPLET".equals(stagiaire.getStatut())) {
            stagiaire.setStatut("DOCUMENT COMPLET");
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
}