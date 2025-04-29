package gestion.pac.gestionstagiairesbackend.controller;

import gestion.pac.gestionstagiairesbackend.entite.User;
import gestion.pac.gestionstagiairesbackend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000") // Très important pour React
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping(consumes = "multipart/form-data")
    public User createUser(
            @RequestParam String civilite,
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String contactUrgent,
            @RequestParam List<String> directions,
            @RequestParam MultipartFile cv,
            @RequestParam MultipartFile lettre,
            @RequestParam boolean consentement
    ) throws IOException {
        User user = new User();
        user.setCivilite(civilite);
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setContactUrgent(contactUrgent);
        user.setDirections(directions);
        user.setConsentement(consentement);

        // Sauver les fichiers
        File uploadDir = new File("uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Nettoyer les noms de fichiers (simplement)
        String cleanCvName = cv.getOriginalFilename().replaceAll("\\s+", "_");
        String cleanLettreName = lettre.getOriginalFilename().replaceAll("\\s+", "_");

        // Construire les chemins complets
        String cvPath = Paths.get(uploadDir.getAbsolutePath(), cleanCvName).toString();
        String lettrePath = Paths.get(uploadDir.getAbsolutePath(), cleanLettreName).toString();

        // Sauver les fichiers
        cv.transferTo(new File(cvPath));
        lettre.transferTo(new File(lettrePath));

        // Enregistrer les chemins dans l'utilisateur
        user.setCvPath(cvPath);
        user.setLettrePath(lettrePath);

        return userRepository.save(user);
    }
}
