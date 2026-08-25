package inpg.sn.lms.accreditation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.accreditation.entity.CritereAccreditation;

@Repository
public interface CritereAccreditationRepository extends JpaRepository<CritereAccreditation, Long> {

    List<CritereAccreditation> findByActifTrue();
}
