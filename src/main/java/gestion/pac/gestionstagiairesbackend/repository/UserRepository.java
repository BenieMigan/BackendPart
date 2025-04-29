package gestion.pac.gestionstagiairesbackend.repository;

import gestion.pac.gestionstagiairesbackend.entite.User; // Correction ici
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
