package gestion.pac.gestionstagiairesbackend.repository;

import gestion.pac.gestionstagiairesbackend.entite.DemandeAbsence;
import gestion.pac.gestionstagiairesbackend.entite.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeAbsenceRepository extends JpaRepository<DemandeAbsence, Long> {
    List<DemandeAbsence> findByStagiaire(User stagiaire);
    List<DemandeAbsence> findByEncadreur(User encadreur);
    List<DemandeAbsence> findByEncadreurAndStatut(User encadreur, String statut);
}