package inpg.sn.lms.recrutementformateur.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutementformateur.entity.CandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutCandidatureFormateur;

@Repository
public interface CandidatureFormateurRepository extends JpaRepository<CandidatureFormateur, Long> {

    List<CandidatureFormateur> findByCandidatIdOrderByDateSoumissionDesc(Long candidatUserId);

    List<CandidatureFormateur> findAllByOrderByDateSoumissionDesc();

    List<CandidatureFormateur> findByOffreIdOrderByDateSoumissionDesc(Long offreId);

    List<CandidatureFormateur> findByStatutOrderByDateSoumissionDesc(EnumStatutCandidatureFormateur statut);

    /** Candidature active (non terminée) de ce candidat sur cette offre, s'il y en a une. */
    List<CandidatureFormateur> findByOffreIdAndCandidatIdAndStatutNot(Long offreId, Long candidatUserId,
            EnumStatutCandidatureFormateur statut);

    long countByOffreId(Long offreId);
}
