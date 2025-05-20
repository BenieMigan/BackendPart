package gestion.pac.gestionstagiairesbackend.repository;

import gestion.pac.gestionstagiairesbackend.entite.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import gestion.pac.gestionstagiairesbackend.entite.Direction;

public interface DirectionRepository extends JpaRepository<Direction, Long> {
    Optional<Direction> findByNom(String nom);
}
