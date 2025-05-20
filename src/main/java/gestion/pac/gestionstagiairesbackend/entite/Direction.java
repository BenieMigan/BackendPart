package gestion.pac.gestionstagiairesbackend.entite;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Direction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private int placesTotales;
    private int placesOccupees;

    public Direction() {
    }

    public Direction(Long id, String nom, int placesTotales, int placesOccupees) {
        this.id = id;
        this.nom = nom;
        this.placesTotales = placesTotales;
        this.placesOccupees = placesOccupees;
    }
    public Direction(String nom, int placesTotales, int placesOccupees) {
        this.nom = nom;
        this.placesTotales = placesTotales;
        this.placesOccupees = placesOccupees;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getPlacesTotales() {
        return placesTotales;
    }

    public void setPlacesTotales(int placesTotales) {
        this.placesTotales = placesTotales;
    }

    public int getPlacesOccupees() {
        return placesOccupees;
    }

    public void setPlacesOccupees(int placesOccupees) {
        this.placesOccupees = placesOccupees;
    }
}