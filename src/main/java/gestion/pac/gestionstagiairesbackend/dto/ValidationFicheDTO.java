package gestion.pac.gestionstagiairesbackend.dto;

import java.time.LocalDate;

public class ValidationFicheDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private boolean confirmerPeriodeStagiaire; // Nouveau champ


    public boolean isConfirmerPeriodeStagiaire() {
        return confirmerPeriodeStagiaire;
    }

    public void setConfirmerPeriodeStagiaire(boolean confirmerPeriodeStagiaire) {
        this.confirmerPeriodeStagiaire = confirmerPeriodeStagiaire;
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
}