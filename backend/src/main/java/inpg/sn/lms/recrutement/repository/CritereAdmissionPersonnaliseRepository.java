package inpg.sn.lms.recrutement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.recrutement.entity.CritereAdmissionPersonnalise;

public interface CritereAdmissionPersonnaliseRepository extends JpaRepository<CritereAdmissionPersonnalise, Long> {

    List<CritereAdmissionPersonnalise> findBySessionCandidatureIdOrderByOrdreAsc(Long sessionCandidatureId);

    List<CritereAdmissionPersonnalise> findBySessionCandidatureIdAndActifTrueOrderByOrdreAsc(Long sessionCandidatureId);
}
