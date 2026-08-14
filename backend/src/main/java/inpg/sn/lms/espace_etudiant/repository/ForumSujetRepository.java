package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.ForumSujet;

@Repository
public interface ForumSujetRepository extends JpaRepository<ForumSujet, Long> {

    /**
     * Sujets accessibles à un étudiant : les salons généraux et ceux rattachés à
     * un cours de son cursus.
     */
    @Query("""
            SELECT s FROM ForumSujet s
            WHERE s.cours IS NULL OR s.cours.id IN :coursIds
            ORDER BY s.epingle DESC, s.dateCreation DESC
            """)
    List<ForumSujet> findVisiblesPour(@Param("coursIds") List<Long> coursIds);

    /** Repli lorsque l'étudiant n'a encore aucun cours autorisé. */
    @Query("SELECT s FROM ForumSujet s WHERE s.cours IS NULL ORDER BY s.epingle DESC, s.dateCreation DESC")
    List<ForumSujet> findGeneraux();

    List<ForumSujet> findByCoursIdOrderByEpingleDescDateCreationDesc(Long coursId);
}
