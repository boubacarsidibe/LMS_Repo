package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;
import java.util.List;

public interface SupportCoursRepository extends JpaRepository<SupportCours, Long> {

    long countByCoursId(Long coursId);

    List<SupportCours> findByCoursIdOrderByDatePublicationDesc(Long coursId);

    void deleteByCoursId(Long coursId);
}
