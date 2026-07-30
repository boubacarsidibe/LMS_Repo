package inpg.sn.lms.recrutement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;

@Repository
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    Optional<Candidature> findByNumeroDossier(String numeroDossier);

    List<Candidature> findByCandidatId(Long candidatId);

    List<Candidature> findByStatut(EnumStatutCandidature statut);

    List<Candidature> findBySessionCandidatureId(Long sessionId);

    List<Candidature> findByCandidatIdAndStatut(Long candidatId, EnumStatutCandidature statut);
}