package inpg.sn.lms.espace_etudiant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;
import inpg.sn.lms.espace_etudiant.repository.RetourEvaluationRepository;

@Service
public class RetourEvaluationService {

    private final RetourEvaluationRepository retourEvaluationRepository;

    public RetourEvaluationService(RetourEvaluationRepository retourEvaluationRepository) {
        this.retourEvaluationRepository = retourEvaluationRepository;
    }

    public List<RetourEvaluation> getByCoursIds(List<Long> coursIds) {
        if (coursIds.isEmpty()) {
            return List.of();
        }
        return retourEvaluationRepository.findByCours_IdIn(coursIds);
    }

    public Optional<RetourEvaluation> findById(Long id) {
        return retourEvaluationRepository.findById(id);
    }

    public RetourEvaluation save(RetourEvaluation retour) {
        return retourEvaluationRepository.save(retour);
    }
}
