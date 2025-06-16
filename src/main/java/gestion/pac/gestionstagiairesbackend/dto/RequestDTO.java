package gestion.pac.gestionstagiairesbackend.dto;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RequestDTO {

        private String civilite;
        private String nom;
        private String prenom;

        private String typeStage;

        private String telephone;
        @Column(unique = true)
        private String email;
        @Column(name = "last_login")
        private LocalDateTime lastLogin;


        @Column(name = "first_login")
        private boolean firstLogin = false;

        @Column(name = "reset_password_otp")
        private String resetPasswordOtp;

        @Column(name = "otp_expiry")
        private LocalDateTime otpExpiry;
        private String password;
        private String role = "STAGIAIRE"; // ou "RH", "SECRETAIRE", "CHEF_SERVICE", "ENCADREUR"

        // Pour les secrétaires et chefs de service
        private String direction; // La direction à laquelle ils appartiennent

        // Pour les encadreurs
        private Long chefServiceId; // Référence au chef de service

        // Pour les stagiaires
        private Long secretaireId; // Référence à la secrétaire
        private Long encadreurId; // Référence à l'encadreur
        private String contactUrgent;
        private String ficheAssurancePath; // Chemin vers le fichier uploadé
        @ElementCollection
        private List<String> directions;

        private String cvPath;
        private String lettrePath;
        private Boolean consentement = false; // Changé de boolean à Boolean
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
        private String service;
        private boolean ficheAssuranceValidee;
        private boolean generatedDocsPath;


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

        public String getTypeStage() {
            return typeStage;
        }

        public void setTypeStage(String typeStage) {
            this.typeStage = typeStage;
        }

        public String getTelephone() {
            return telephone;
        }

        public void setTelephone(String telephone) {
            this.telephone = telephone;
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

        public boolean isFirstLogin() {
            return firstLogin;
        }

        public void setFirstLogin(boolean firstLogin) {
            this.firstLogin = firstLogin;
        }

        public String getResetPasswordOtp() {
            return resetPasswordOtp;
        }

        public void setResetPasswordOtp(String resetPasswordOtp) {
            this.resetPasswordOtp = resetPasswordOtp;
        }

        public LocalDateTime getOtpExpiry() {
            return otpExpiry;
        }

        public void setOtpExpiry(LocalDateTime otpExpiry) {
            this.otpExpiry = otpExpiry;
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

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public Long getChefServiceId() {
            return chefServiceId;
        }

        public void setChefServiceId(Long chefServiceId) {
            this.chefServiceId = chefServiceId;
        }

        public Long getSecretaireId() {
            return secretaireId;
        }

        public void setSecretaireId(Long secretaireId) {
            this.secretaireId = secretaireId;
        }

        public Long getEncadreurId() {
            return encadreurId;
        }

        public void setEncadreurId(Long encadreurId) {
            this.encadreurId = encadreurId;
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

        public String getAlerte() {
            return alerte;
        }

        public void setAlerte(String alerte) {
            this.alerte = alerte;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public boolean isFicheAssuranceValidee() {
            return ficheAssuranceValidee;
        }

        public void setFicheAssuranceValidee(boolean ficheAssuranceValidee) {
            this.ficheAssuranceValidee = ficheAssuranceValidee;
        }

        public boolean isGeneratedDocsPath() {
            return generatedDocsPath;
        }

        public void setGeneratedDocsPath(boolean generatedDocsPath) {
            this.generatedDocsPath = generatedDocsPath;
        }
    }


