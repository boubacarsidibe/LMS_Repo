package inpg.sn.lms.recrutementformateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutementformateur.entity.LangueCandidatureFormateur;

@Repository
public interface LangueCandidatureFormateurRepository extends JpaRepository<LangueCandidatureFormateur, Long> {

    List<LangueCandidatureFormateur> findByCandidatureId(Long candidatureId);
}
