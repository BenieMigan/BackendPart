package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.dto.ReponseAbsenceDTO;
import gestion.pac.gestionstagiairesbackend.entite.DemandeAbsence;
import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.DemandeAbsenceRepository;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import gestion.pac.gestionstagiairesbackend.service.EmailService;
import gestion.pac.gestionstagiairesbackend.service.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/encadreur")
@CrossOrigin(origins = "http://localhost:3000")
public class EncadreurController {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final DemandeAbsenceRepository demandeAbsenceRepository;
    private final EmailService emailService;


    public EncadreurController(UserRepository userRepository,
                               JwtTokenService jwtTokenService,
                               DemandeAbsenceRepository demandeAbsenceRepository,
                               EmailService emailService) {

        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.demandeAbsenceRepository = demandeAbsenceRepository;
        this.emailService = emailService;

    }

    // Récupérer les stagiaires sous ma supervision
    @GetMapping("/stagiaires")
    public ResponseEntity<?> getStagiaires(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long encadreurId = jwtTokenService.validateAndGetUserId(token);
            User encadreur = userRepository.findById(encadreurId)
                    .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));

            if (!"ENCADREUR".equals(encadreur.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux encadreurs");
            }

            List<User> stagiaires = userRepository.findByRoleAndEncadreurId("STAGIAIRE", encadreurId);

            List<Map<String, Object>> result = stagiaires.stream()
                    .map(s -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", s.getId());
                        map.put("nomComplet", s.getNom() + " " + s.getPrenom());
                        map.put("email", s.getEmail());
                        map.put("filiere", s.getFiliere());
                        map.put("dateDebut", s.getDateDebut());
                        map.put("dateFin", s.getDateFin());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }


    // Endpoint pour récupérer les demandes en attente
    @GetMapping("/demandes-absence")
    public ResponseEntity<?> getDemandesAbsenceEnAttente(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long encadreurId = jwtTokenService.validateAndGetUserId(token);
            User encadreur = userRepository.findById(encadreurId)
                    .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));

            if (!"ENCADREUR".equals(encadreur.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux encadreurs");
            }

            List<DemandeAbsence> demandes = demandeAbsenceRepository.findByEncadreurAndStatut(encadreur, "EN_ATTENTE");

            return ResponseEntity.ok(demandes.stream().map(d -> Map.of(
                    "id", d.getId(),
                    "stagiaireId", d.getStagiaire().getId(),
                    "stagiaireNom", d.getStagiaire().getNom() + " " + d.getStagiaire().getPrenom(),
                    "dateDebut", d.getDateDebut(),
                    "dateFin", d.getDateFin(),
                    "motif", d.getMotif(),
                    "dateCreation", d.getDateCreation()
            )).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Endpoint pour répondre à une demande
    @PostMapping("/repondre-demande-absence")
    public ResponseEntity<?> repondreDemandeAbsence(
            @RequestBody ReponseAbsenceDTO reponseDTO,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long encadreurId = jwtTokenService.validateAndGetUserId(token);
            User encadreur = userRepository.findById(encadreurId)
                    .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));

            if (!"ENCADREUR".equals(encadreur.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux encadreurs");
            }

            DemandeAbsence demande = demandeAbsenceRepository.findById(reponseDTO.getDemandeId())
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

            if (!demande.getEncadreur().getId().equals(encadreurId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vous n'êtes pas l'encadreur de ce stagiaire");
            }

            if (!"EN_ATTENTE".equals(demande.getStatut())) {
                return ResponseEntity.badRequest().body("Cette demande a déjà été traitée");
            }

            demande.setStatut(reponseDTO.getStatut());
            demande.setCommentaire(reponseDTO.getCommentaire());
            demande.setDateReponse(LocalDateTime.now());

            demandeAbsenceRepository.save(demande);

            // Après avoir sauvegardé la réponse
            emailService.sendNotificationReponseAbsence(demande.getStagiaire(), demande);

            return ResponseEntity.ok(Map.of(
                    "message", "Réponse enregistrée avec succès",
                    "demandeId", demande.getId(),
                    "statut", demande.getStatut()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Endpoint pour voir l'historique des demandes traitées
    @GetMapping("/historique-demandes-absence")
    public ResponseEntity<?> getHistoriqueDemandesAbsence(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long encadreurId = jwtTokenService.validateAndGetUserId(token);
            User encadreur = userRepository.findById(encadreurId)
                    .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));

            if (!"ENCADREUR".equals(encadreur.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux encadreurs");
            }

            List<DemandeAbsence> demandes = demandeAbsenceRepository.findByEncadreur(encadreur);

            return ResponseEntity.ok(demandes.stream().map(d -> Map.of(
                    "id", d.getId(),
                    "stagiaireId", d.getStagiaire().getId(),
                    "stagiaireNom", d.getStagiaire().getNom() + " " + d.getStagiaire().getPrenom(),
                    "dateDebut", d.getDateDebut(),
                    "dateFin", d.getDateFin(),
                    "motif", d.getMotif(),
                    "statut", d.getStatut(),
                    "commentaire", d.getCommentaire(),
                    "dateCreation", d.getDateCreation(),
                    "dateReponse", d.getDateReponse()
            )).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Récupérer mon profil
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long encadreurId = jwtTokenService.validateAndGetUserId(token);
            User encadreur = userRepository.findById(encadreurId)
                    .orElseThrow(() -> new RuntimeException("Encadreur non trouvé"));

            if (!"ENCADREUR".equals(encadreur.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès réservé aux encadreurs");
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", encadreur.getId());
            profile.put("civilite", encadreur.getCivilite());
            profile.put("nom", encadreur.getNom());
            profile.put("prenom", encadreur.getPrenom());
            profile.put("email", encadreur.getEmail());
            profile.put("telephone", encadreur.getTelephone());
            profile.put("service", encadreur.getService());
            profile.put("direction", encadreur.getDirection());

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }


}