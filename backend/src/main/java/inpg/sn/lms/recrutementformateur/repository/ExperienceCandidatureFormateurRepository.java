package inpg.sn.lms.recrutementformateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutementformateur.entity.ExperienceCandidatureFormateur;

@Repository
public interface ExperienceCandidatureFormateurRepository extends JpaRepository<ExperienceCandidatureFormateur, Long> {

    List<ExperienceCandidatureFormateur> findByCandidatureId(Long candidatureId);
}
