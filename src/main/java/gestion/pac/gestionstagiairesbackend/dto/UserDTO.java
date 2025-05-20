package gestion.pac.gestionstagiairesbackend.dto;

import gestion.pac.gestionstagiairesbackend.entite.User;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


public class UserDTO {

    private Long id;
    private String civilite;
    private String nom;
    private String prenom;
    private String email;
    private String contactUrgent;
    @ElementCollection
    private List<String> directions;
    private MultipartFile cv;
    private MultipartFile lettre;
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

    private String telephone;


    public UserDTO() {
    }

    public UserDTO(Long id, String civilite, String nom, String prenom, String email, String contactUrgent, List<String> directions, MultipartFile cv, MultipartFile lettre, Boolean consentement, String typeStage, String nomEtablissement, String adresseEtablissement, String message, String statut, String filiere, String anneeAcademique, LocalDate dateDebut, LocalDate dateFin,String telephone) {
        this.id = id;
        this.civilite = civilite;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.contactUrgent = contactUrgent;
        this.directions = directions;
        this.cv = cv;
        this.lettre = lettre;
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

    public MultipartFile getCv() {
        return cv;
    }

    public void setCv(MultipartFile cv) {
        this.cv = cv;
    }

    public MultipartFile getLettre() {
        return lettre;
    }

    public void setLettre(MultipartFile lettre) {
        this.lettre = lettre;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
}
