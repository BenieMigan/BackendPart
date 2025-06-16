package gestion.pac.gestionstagiairesbackend.dto;

public class CreateAccountByRHDTO {
    private String civilite;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role;
    private String telephone;

    // Champs optionnels selon le rôle
    private String direction;
    private String service;
    private Long chefServiceId; // Pour les encadreurs

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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Long getChefServiceId() {
        return chefServiceId;
    }

    public void setChefServiceId(Long chefServiceId) {
        this.chefServiceId = chefServiceId;
    }
}
