
package com.neogeo.tracking.repository;

import com.neogeo.tracking.model.Surveyor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurveyorRepository extends JpaRepository<Surveyor, String> {

    // Basic filtered queries
    List<Surveyor> findByCity(String city);
    List<Surveyor> findByProjectName(String projectName);
    List<Surveyor> findByCityAndProjectName(String city, String projectName);
    
    // Case-insensitive search
    List<Surveyor> findByCityContainingIgnoreCase(String city);
    List<Surveyor> findByProjectNameContainingIgnoreCase(String projectName);
    List<Surveyor> findByCityContainingIgnoreCaseAndProjectNameContainingIgnoreCase(String city, String projectName);
    
    // Username queries
    Optional<Surveyor> findByUsername(String username);
    boolean existsByUsername(String username);
    
    // Custom queries
    @Query("SELECT s FROM Surveyor s WHERE " +
           "LOWER(s.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.projectName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Surveyor> searchByAnyField(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT s FROM Surveyor s WHERE " +
           "NOT LOWER(s.id) LIKE '%admin%' AND " +
           "(s.username IS NULL OR NOT LOWER(s.username) LIKE '%admin%')")
    List<Surveyor> findAllNonAdminSurveyors();
    
    @Query("SELECT DISTINCT s.city FROM Surveyor s WHERE s.city IS NOT NULL")
    List<String> findAllDistinctCities();
    
    @Query("SELECT DISTINCT s.projectName FROM Surveyor s WHERE s.projectName IS NOT NULL")
    List<String> findAllDistinctProjects();
    
    @Query("SELECT COUNT(s) > 0 FROM Surveyor s WHERE s.username = :username AND s.id != :excludeId")
    boolean existsByUsernameExcludingId(@Param("username") String username, 
                                      @Param("excludeId") String excludeId);
}

