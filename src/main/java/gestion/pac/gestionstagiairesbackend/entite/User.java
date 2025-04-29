package gestion.pac.gestionstagiairesbackend.entite;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users") // sans guillemets cette fois
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String civilite;
    private String nom;
    private String prenom;
    private String email;
    private String contactUrgent;

    @ElementCollection
    private List<String> directions;

    private String cvPath;
    private String lettrePath;

    private boolean consentement;

    public User() {
    }

    public User(Long id, String civilite, String nom, String prenom, String email, String contactUrgent, List<String> directions, String cvPath, String lettrePath, boolean consentement) {
        this.id = id;
        this.civilite = civilite;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.contactUrgent = contactUrgent;
        this.directions = directions;
        this.cvPath = cvPath;
        this.lettrePath = lettrePath;
        this.consentement = consentement;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCivilite() {
        return civilite;
    }

    public void setCivilite(String civilite) {
        this.civilite = civilite;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactUrgent() {
        return contactUrgent;
    }

    public void setContactUrgent(String contactUrgent) {
        this.contactUrgent = contactUrgent;
    }

    public List<String> getDirections() {
        return directions;
    }

    public void setDirections(List<String> directions) {
        this.directions = directions;
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    public String getLettrePath() {
        return lettrePath;
    }

    public void setLettrePath(String lettrePath) {
        this.lettrePath = lettrePath;
    }

    public boolean isConsentement() {
        return consentement;
    }

    public void setConsentement(boolean consentement) {
        this.consentement = consentement;
    }
}
