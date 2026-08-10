package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.Formation;

public interface FormationRepository extends JpaRepository<Formation, Long> {

}
