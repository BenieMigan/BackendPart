package gestion.pac.gestionstagiairesbackend.entite;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private String telephone;
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
    private String noteServicePath;
    private String demandeStagePath;
    @Column(name = "alerte")
    private String alerte;
    private boolean ficheAssuranceValidee;
    private boolean generatedDocsPath;


    public User() {
    }

    public User(Long id, String civilite, String nom, String prenom, String email, String password, String role, String contactUrgent, List<String> directions, String cvPath, String lettrePath, Boolean consentement, String typeStage, String nomEtablissement, String adresseEtablissement, String message, String statut, String filiere, String anneeAcademique, LocalDate dateDebut, LocalDate dateFin, String ficheAssurancePath, LocalDateTime lastLogin, LocalDateTime dateSoumission, String telephone, String noteServicePath, String demandeStagePath, String alerte, boolean ficheAssuranceValidee,boolean generatedDocsPath) {
        this.id = id;
        this.civilite = civilite;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.contactUrgent = contactUrgent;
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
        this.ficheAssurancePath = ficheAssurancePath;
        this.lastLogin = lastLogin;
        this.dateSoumission = dateSoumission;
        this.telephone = telephone;
        this.noteServicePath = noteServicePath;
        this.demandeStagePath = demandeStagePath;
        this.alerte = alerte;
        this.ficheAssuranceValidee = ficheAssuranceValidee;
        this.generatedDocsPath = generatedDocsPath;




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

    public String getAlerte() {
        return alerte;
    }

    public void setAlerte(String alerte) {
        this.alerte = alerte;
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

    public String getFicheAssurancePath() {
        return ficheAssurancePath;
    }

    public void setFicheAssurancePath(String ficheAssurancePath) {
        this.ficheAssurancePath = ficheAssurancePath;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
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

    public String getNoteServicePath() {
        return noteServicePath;
    }

    public void setNoteServicePath(String noteServicePath) {
        this.noteServicePath = noteServicePath;
    }

    public String getDemandeStagePath() {
        return demandeStagePath;
    }

    public void setDemandeStagePath(String demandeStagePath) {
        this.demandeStagePath = demandeStagePath;
    }


    public void setFicheAssuranceValidee(boolean ficheAssuranceValidee) {
        this.ficheAssuranceValidee = ficheAssuranceValidee;
    }

    public boolean isFicheAssuranceValidee() {
        return "FICHE_ASSURANCE_VALIDEE".equals(this.statut);
    }

    public boolean isGeneratedDocsPath() {
        return generatedDocsPath;
    }

    public void setGeneratedDocsPath(boolean generatedDocsPath) {
        this.generatedDocsPath = generatedDocsPath;
    }
}

