package inpg.sn.lms.recrutementformateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutementformateur.entity.PieceCandidatureFormateur;

@Repository
public interface PieceCandidatureFormateurRepository extends JpaRepository<PieceCandidatureFormateur, Long> {

    List<PieceCandidatureFormateur> findByCandidatureId(Long candidatureId);
}
