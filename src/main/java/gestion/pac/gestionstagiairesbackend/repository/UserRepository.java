package gestion.pac.gestionstagiairesbackend.repository;

import gestion.pac.gestionstagiairesbackend.entite.User; // Correction ici
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);                 // <- manquante
    Optional<User> findByEmail(String email);// <- manquante


    List<User> findByRole(String role);

    // Compter les utilisateurs par rôle
    long countByRole(String role);

    // Compter les utilisateurs par statut
    long countByStatut(String statut);
    long countByStatutIn(List<String> statuts);

    // Trouver les utilisateurs par statut
    List<User> findByStatut(String statut);

    // Trouver les utilisateurs avec fin de stage proche et statut spécifique
    List<User> findByDateFinBetweenAndStatut(LocalDate start, LocalDate end, String statut);

    // Statistiques par département (requête native)
    @Query(value = "SELECT d.nom AS department, COUNT(DISTINCT u.id) AS count " +
            "FROM users u " +
            "JOIN user_directions ud ON u.id = ud.user_id " +
            "JOIN direction d ON ud.user_id = d.id " +  // Notez "direction" au singulier
            "WHERE u.statut = 'DOCUMENT_COMPLET' " +
            "GROUP BY d.nom", nativeQuery = true)
    List<Map<String, Object>> getStatsByDepartment();

    // Derniers stagiaires ajoutés
    List<User> findTop5ByRoleOrderByDateSoumissionDesc(String role);


    // Ajoutez ces nouvelles méthodes
    @Query(value = "SELECT d.nom AS department, d.places_totales AS total, d.places_occupees AS occupied " +
            "FROM direction d", nativeQuery = true)
    List<Map<String, Object>> getDepartmentCapacity();

    @Query(value = "SELECT " +
            "EXTRACT(QUARTER FROM u.date_soumission) AS quarter, " +
            "EXTRACT(YEAR FROM u.date_soumission) AS year, " +
            "COUNT(u.id) AS count " +
            "FROM users u " +
            "WHERE u.role = 'STAGIAIRE' " +
            "GROUP BY year, quarter " +
            "ORDER BY year, quarter", nativeQuery = true)
    List<Map<String, Object>> getDemandesByQuarter();

    @Query(value = "SELECT " +
            "EXTRACT(YEAR FROM u.date_soumission) AS year, " +
            "COUNT(u.id) AS count " +
            "FROM users u " +
            "WHERE u.role = 'STAGIAIRE' " +
            "GROUP BY year " +
            "ORDER BY year", nativeQuery = true)
    List<Map<String, Object>> getDemandesByYear();

    @Query(value = "SELECT * FROM users u " +
            "WHERE u.role = 'STAGIAIRE' " +
            "AND EXTRACT(QUARTER FROM u.date_soumission) = :quarter " +
            "AND EXTRACT(YEAR FROM u.date_soumission) = :year", nativeQuery = true)
    List<User> findDemandesByQuarterAndYear(@Param("quarter") int quarter, @Param("year") int year);



    @Query(value = "SELECT * FROM users u " +
            "WHERE u.statut = 'DOCUMENT_COMPLET' " +
            "AND EXTRACT(YEAR FROM u.date_soumission) = :year", nativeQuery = true)
    List<User> findByStatutAndYear(@Param("statut") String statut, @Param("year") int year);

    @Query(value = "SELECT * FROM users u " +
            "WHERE u.statut = 'DOCUMENT_COMPLET' " +
            "AND EXTRACT(YEAR FROM u.date_soumission) = :year " +
            "AND EXTRACT(QUARTER FROM u.date_soumission) = :quarter", nativeQuery = true)
    List<User> findByStatutAndQuarter(@Param("statut") String statut,
                                      @Param("year") int year,
                                      @Param("quarter") int quarter);


    // Ajoutez ces nouvelles requêtes dans UserRepository.java

    @Query(value = "SELECT " +
            "EXTRACT(YEAR FROM u.date_soumission) AS year, " +
            "COUNT(u.id) AS count " +
            "FROM users u " +
            "WHERE u.statut = 'DOCUMENT_COMPLET' " +  // Seulement les dossiers finalisés
            "GROUP BY year " +
            "ORDER BY year", nativeQuery = true)
    List<Map<String, Object>> getCompletedByYear();

    @Query(value = "SELECT " +
            "EXTRACT(QUARTER FROM u.date_soumission) AS quarter, " +
            "EXTRACT(YEAR FROM u.date_soumission) AS year, " +
            "COUNT(u.id) AS count " +
            "FROM users u " +
            "WHERE u.statut = 'DOCUMENT_COMPLET' " +  // Seulement les dossiers finalisés
            "GROUP BY year, quarter " +
            "ORDER BY year, quarter", nativeQuery = true)
    List<Map<String, Object>> getCompletedByQuarter();


}