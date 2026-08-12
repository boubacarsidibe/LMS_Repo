package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.Seance;

public interface SeanceRepository extends JpaRepository<Seance, Long> {
    List<Seance> findByCours_Formateurs_IdOrderByDateHeureAsc(Long formateurId);
}