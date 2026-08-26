package inpg.sn.lms.accreditation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.accreditation.entity.AvisAccreditation;

@Repository
public interface AvisAccreditationRepository extends JpaRepository<AvisAccreditation, Long> {

    List<AvisAccreditation> findByDemandeIdOrderByDateAvisAsc(Long demandeId);

    Optional<AvisAccreditation> findByDemandeIdAndEvaluateurId(Long demandeId, Long evaluateurId);
}
