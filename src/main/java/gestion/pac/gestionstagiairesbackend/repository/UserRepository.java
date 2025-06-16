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

    boolean existsByRoleAndDirection(String role, String direction);

    boolean existsByRoleAndDirectionAndService(String role, String direction, String service);

    boolean existsByRoleAndServiceAndDirectionNot(String role, String service, String direction);

    List<User> findByRoleAndChefServiceId(String role, Long chefServiceId);

    @Query("SELECT DISTINCT u FROM User u WHERE u.role = 'CHEF_SERVICE' " +
            "AND u.direction = :direction " +
            "AND EXISTS (SELECT 1 FROM User s WHERE s.role = 'STAGIAIRE' AND s.chefServiceId = u.id)")
    List<User> findChefsServiceWithStagiaires(@Param("direction") String direction);


    // Nouvelle méthode pour trouver par service
    List<User> findByService(String service);
    // Trouver les utilisateurs par rôle et statut
    List<User> findByRoleAndStatut(String role, String statut);

    List<User> findByRoleAndEncadreurId(String role, Long encadreurId);


    // Trouver les secrétaires par direction
    List<User> findByRoleAndDirection(String role, String direction);

    // Trouver les stagiaires affectés à une secrétaire
    List<User> findByRoleAndSecretaireId(String role, Long secretaireId);

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

    // Pour les demandes de pause/reprise
    List<User> findByPauseRequestDateIsNotNull();
    List<User> findByRepriseRequestDateIsNotNull();

    // Pour compter par statut de stage
    long countByStageStatus(User.StageStatus status);

    // Pour compter les demandes de pause/reprise
    long countByPauseRequestDateIsNotNull();
    long countByRepriseRequestDateIsNotNull();

    // Pour les alertes
    List<User> findByDateFinBetweenAndStageStatus(LocalDate start, LocalDate end, User.StageStatus status);
    List<User> findByDateFinAndStageStatus(LocalDate date, User.StageStatus status);
    @Query("SELECT DISTINCT e FROM User e WHERE e.role = 'ENCADREUR' " +
            "AND e.chefServiceId = :chefServiceId " +
            "AND EXISTS (SELECT 1 FROM User s WHERE s.role = 'STAGIAIRE' AND s.encadreurId = e.id)")
    List<User> findEncadreursWithStagiaires(@Param("chefServiceId") Long chefServiceId);

    @Query("SELECT DISTINCT s FROM User s WHERE s.role = 'SECRETAIRE' AND EXISTS " +
            "(SELECT st FROM User st WHERE st.secretaireId = s.id AND st.role = 'STAGIAIRE')")
    List<User> findSecretairesWithStagiaires();


























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