package gestion.pac.gestionstagiairesbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class DocumentAdministratif {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;  // Exemple : "Convention de stage", "Attestation de présence"
    private LocalDate datecreation;
    private String chemindocument; // URL ou chemin du fichier généré

    @ManyToOne
    @JoinColumn(name = "stagiaire_id", referencedColumnName = "id")
    private Stagiaire stagiaire;

}




