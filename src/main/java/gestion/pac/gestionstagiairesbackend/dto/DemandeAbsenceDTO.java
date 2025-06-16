package gestion.pac.gestionstagiairesbackend.dto;

import java.time.LocalDate;

public class DemandeAbsenceDTO {
    private Long stagiaireId;
    private Long encadreurId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String motif;

    // Getters et setters
    public Long getStagiaireId() {
        return stagiaireId;
    }

    public void setStagiaireId(Long stagiaireId) {
        this.stagiaireId = stagiaireId;
    }

    public Long getEncadreurId() {
        return encadreurId;
    }

    public void setEncadreurId(Long encadreurId) {
        this.encadreurId = encadreurId;
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

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}