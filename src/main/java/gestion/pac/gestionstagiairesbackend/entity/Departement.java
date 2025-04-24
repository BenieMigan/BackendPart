package gestion.pac.gestionstagiairesbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;

    @OneToMany(mappedBy = "departement", fetch = FetchType.LAZY)
    private List<DemandeStage> demandestage;


}






