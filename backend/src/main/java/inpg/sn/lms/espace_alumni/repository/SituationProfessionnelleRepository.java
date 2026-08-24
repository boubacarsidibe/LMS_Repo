package inpg.sn.lms.espace_alumni.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_alumni.entity.SituationProfessionnelle;

@Repository
public interface SituationProfessionnelleRepository extends JpaRepository<SituationProfessionnelle, Long> {
	List<SituationProfessionnelle> findByAlumniProfilIdOrderByDateDebutDesc(Long alumniProfilId);
}
