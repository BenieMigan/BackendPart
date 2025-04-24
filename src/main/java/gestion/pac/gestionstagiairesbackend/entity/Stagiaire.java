package gestion.pac.gestionstagiairesbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class Stagiaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String civilite;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String contacturgent;
    private LocalDate datedebutstage;
    private LocalDate datefinstage;
    private boolean demandevalidee;
    private String ficheAssurance; // Stocke le chemin ou l'URL du fichier

    @OneToMany(mappedBy = "stagiaire", fetch = FetchType.LAZY)
    private List<DemandeStage> demandestage;

    @OneToMany(mappedBy = "stagiaire", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DocumentAdministratif> documentadministratif;

}
