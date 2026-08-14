package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.EvaluationCours;

@Repository
public interface EvaluationCoursRepository extends JpaRepository<EvaluationCours, Long> {

    Optional<EvaluationCours> findByEtudiantIdAndCoursId(Long etudiantId, Long coursId);

    List<EvaluationCours> findByEtudiantIdOrderByDateSoumissionDesc(Long etudiantId);

    List<EvaluationCours> findByCoursIdOrderByDateSoumissionDesc(Long coursId);

    long countByCoursId(Long coursId);

    /** Note moyenne du cours, {@code null} tant qu'aucune évaluation n'a été soumise. */
    @Query("SELECT AVG(e.note) FROM EvaluationCours e WHERE e.cours.id = :coursId")
    Double moyenneParCours(@Param("coursId") Long coursId);
}
