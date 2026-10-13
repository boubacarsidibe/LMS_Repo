package inpg.sn.lms.recrutementformateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutementformateur.entity.FormationCandidatureFormateur;

@Repository
public interface FormationCandidatureFormateurRepository extends JpaRepository<FormationCandidatureFormateur, Long> {

    List<FormationCandidatureFormateur> findByCandidatureId(Long candidatureId);
}
