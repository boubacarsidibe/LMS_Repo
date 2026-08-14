package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;

public interface RetourEvaluationRepository extends JpaRepository<RetourEvaluation, Long> {

    @EntityGraph(attributePaths = "cours")
    List<RetourEvaluation> findByCours_IdIn(List<Long> coursIds);

    @Override
    @EntityGraph(attributePaths = {"cours", "cours.formateurs"})
    Optional<RetourEvaluation> findById(Long id);
}
