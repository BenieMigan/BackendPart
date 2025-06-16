package gestion.pac.gestionstagiairesbackend.dto;

public class ReponseAbsenceDTO {
    private Long demandeId;
    private String statut; // "ACCEPTEE" ou "REFUSEE"
    private String commentaire;

    // Getters et setters
    public Long getDemandeId() {
        return demandeId;
    }

    public void setDemandeId(Long demandeId) {
        this.demandeId = demandeId;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}