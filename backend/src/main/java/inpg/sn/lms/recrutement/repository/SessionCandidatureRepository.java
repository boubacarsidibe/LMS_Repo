package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.SessionCandidature;

@Repository
public interface SessionCandidatureRepository extends JpaRepository<SessionCandidature, Long> {

    List<SessionCandidature> findByEstOuverteTrue();

    List<SessionCandidature> findByLibelleContainingIgnoreCase(String libelle);

    SessionCandidature findTopByEstOuverteTrueOrderByIdDesc();

    /**
     * Sessions rattachées à un programme. Compté avant toute suppression de
     * programme : la clé étrangère {@code sessions_candidature.program_id}
     * faisait autrement remonter une violation de contrainte en 500, là où
     * l'appelant a besoin de savoir que la suppression est refusée et pourquoi.
     */
    long countByProgram_Id(Long programId);
}
