package gestion.pac.gestionstagiairesbackend.entity;


import jakarta.persistence.*;

import java.util.List;

@Entity
    public class RH {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String nom;
        private String email;
        private String telephone;

        @OneToMany(mappedBy = "rh", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<DemandeStage> demandestage;

    }


