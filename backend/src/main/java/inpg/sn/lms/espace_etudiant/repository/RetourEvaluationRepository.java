package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;

public interface RetourEvaluationRepository extends JpaRepository<RetourEvaluation, Long> {

    /**
     * Les avis épinglés remontent en tête, puis les plus récents.
     *
     * <p>Sans tri explicite, la base renvoyait les lignes dans un ordre arbitraire :
     * épingler un avis ne produisait alors aucun effet visible, et l'onglet
     * « Plus récents » de l'écran formateur n'était pas chronologique.
     */
    @EntityGraph(attributePaths = "cours")
    List<RetourEvaluation> findByCours_IdInOrderByEpingleDescDateCreationDesc(List<Long> coursIds);

    @Override
    @EntityGraph(attributePaths = {"cours", "cours.formateurs"})
    Optional<RetourEvaluation> findById(Long id);
}
