package gestion.pac.gestionstagiairesbackend.dto;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DemandeStageDTO {


    private Long id;

    private String civilite;
    private String nom;
    private String prenom;

    @Column(unique = true)
    private String email;
    @Column(name = "last_login")
    private LocalDateTime lastLogin;


    private String password;
    private String role = "STAGIAIRE"; // ou "RH"

    private String contactUrgent;
    private String ficheAssurancePath; // Chemin vers le fichier uploadé
    @ElementCollection
    private List<String> directions;

    private String cvPath;
    private String lettrePath;
    private Boolean consentement = false; // Changé de boolean à Boolean
    private String typeStage;
    private String nomEtablissement;
    private String adresseEtablissement;
    private String message;
    private String statut = "EN_ATTENTE"; // Valeur par défaut
    private String filiere;        // Ex: "Informatique, Réseaux et Télécommunication"
    private String anneeAcademique; // Ex: "3ème année de Licence Professionnelle"
    private LocalDate dateDebut; //
    private LocalDate dateFin;
    private String genre; // "F" ou "M"
    private LocalDateTime dateSoumission;
    private String telephone; // "F" ou "M"


    public DemandeStageDTO() {
    }

    public DemandeStageDTO(Long id, String civilite, String nom, String prenom, String email, LocalDateTime lastLogin, String password, String role, String contactUrgent, String ficheAssurancePath, List<String> directions, String cvPath, String lettrePath, Boolean consentement, String typeStage, String nomEtablissement, String adresseEtablissement, String message, String statut, String filiere, String anneeAcademique, LocalDate dateDebut, LocalDate dateFin, String genre, LocalDateTime dateSoumission,String telephone) {
        this.id = id;
        this.civilite = civilite;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.lastLogin = lastLogin;
        this.password = password;
        this.role = role;
        this.contactUrgent = contactUrgent;
        this.ficheAssurancePath = ficheAssurancePath;
        this.directions = directions;
        this.cvPath = cvPath;
        this.lettrePath = lettrePath;
        this.consentement = consentement;
        this.typeStage = typeStage;
        this.nomEtablissement = nomEtablissement;
        this.adresseEtablissement = adresseEtablissement;
        this.message = message;
        this.statut = statut;
        this.filiere = filiere;
        this.anneeAcademique = anneeAcademique;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.genre = genre;
        this.dateSoumission = dateSoumission;
        this.telephone = telephone;


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

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContactUrgent() {
        return contactUrgent;
    }

    public void setContactUrgent(String contactUrgent) {
        this.contactUrgent = contactUrgent;
    }

    public String getFicheAssurancePath() {
        return ficheAssurancePath;
    }

    public void setFicheAssurancePath(String ficheAssurancePath) {
        this.ficheAssurancePath = ficheAssurancePath;
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

    public Boolean getConsentement() {
        return consentement;
    }

    public void setConsentement(Boolean consentement) {
        this.consentement = consentement;
    }

    public String getTypeStage() {
        return typeStage;
    }

    public void setTypeStage(String typeStage) {
        this.typeStage = typeStage;
    }

    public String getNomEtablissement() {
        return nomEtablissement;
    }

    public void setNomEtablissement(String nomEtablissement) {
        this.nomEtablissement = nomEtablissement;
    }

    public String getAdresseEtablissement() {
        return adresseEtablissement;
    }

    public void setAdresseEtablissement(String adresseEtablissement) {
        this.adresseEtablissement = adresseEtablissement;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getFiliere() {
        return filiere;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public String getAnneeAcademique() {
        return anneeAcademique;
    }

    public void setAnneeAcademique(String anneeAcademique) {
        this.anneeAcademique = anneeAcademique;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public LocalDateTime getDateSoumission() {
        return dateSoumission;
    }

    public void setDateSoumission(LocalDateTime dateSoumission) {
        this.dateSoumission = dateSoumission;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}