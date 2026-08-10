package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;

public interface SupportCoursRepository extends JpaRepository<SupportCours, Long> {

    long countByCoursId(Long coursId);
}
