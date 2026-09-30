package inpg.sn.lms.recrutementformateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutementformateur.entity.CertificationCandidatureFormateur;

@Repository
public interface CertificationCandidatureFormateurRepository extends JpaRepository<CertificationCandidatureFormateur, Long> {

    List<CertificationCandidatureFormateur> findByCandidatureId(Long candidatureId);
}
