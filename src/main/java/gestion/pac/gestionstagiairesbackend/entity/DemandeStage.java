package gestion.pac.gestionstagiairesbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class DemandeStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate datesoumission;
    private LocalDate datevalidation;
    private Statut statut;

    @ManyToOne
    @JoinColumn(name = "rh_id", referencedColumnName = "id")
    private RH rh;

    @ManyToOne
    @JoinColumn(name = "departement_id", referencedColumnName = "id")
    private Departement departement;

    @ManyToOne
    @JoinColumn(name = "stagiaire_id", referencedColumnName = "id")
    private Stagiaire stagiaire;


}
