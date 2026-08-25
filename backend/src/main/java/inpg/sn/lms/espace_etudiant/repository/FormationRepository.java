package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.Formation;

public interface FormationRepository extends JpaRepository<Formation, Long> {

    /** Catalogue public : seules les formations publiées sont exposées. */
    java.util.List<Formation> findByPublieeTrueOrderByIntituleAsc();

    /** Le code est unique : sert à créer les programmes de démonstration sans doublon. */
    java.util.Optional<Formation> findByCode(String code);

}
