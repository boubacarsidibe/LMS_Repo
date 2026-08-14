package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.Seance;

public interface SeanceRepository extends JpaRepository<Seance, Long> {
    List<Seance> findByCours_Formateurs_IdOrderByDateHeureAsc(Long formateurId);

    /** Calendrier académique d'un étudiant : les séances de ses cours autorisés. */
    List<Seance> findByCoursIdInOrderByDateHeureAsc(List<Long> coursIds);
}