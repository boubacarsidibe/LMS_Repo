package inpg.sn.lms.accreditation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.accreditation.entity.EvaluationCritere;

public interface EvaluationCritereRepository extends JpaRepository<EvaluationCritere, Long> {

    List<EvaluationCritere> findByDemandeIdAndEvaluateurIdOrderByCritereIdAsc(Long demandeId, Long evaluateurId);

    Optional<EvaluationCritere> findByDemandeIdAndCritereIdAndEvaluateurId(Long demandeId, Long critereId,
            Long evaluateurId);
}
