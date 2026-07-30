package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.SessionCandidature;

@Repository
public interface SessionCandidatureRepository extends JpaRepository<SessionCandidature, Long> {

    List<SessionCandidature> findByEstOuverteTrue();

    List<SessionCandidature> findByLibelleContainingIgnoreCase(String libelle);
}