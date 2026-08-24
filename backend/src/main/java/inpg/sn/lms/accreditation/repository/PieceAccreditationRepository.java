package inpg.sn.lms.accreditation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.accreditation.entity.PieceAccreditation;

@Repository
public interface PieceAccreditationRepository extends JpaRepository<PieceAccreditation, Long> {

    List<PieceAccreditation> findByDemandeId(Long demandeId);
}
