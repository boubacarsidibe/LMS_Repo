package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import java.util.List;

public interface CoursRepository extends JpaRepository<Cours, Long> {

    @Override
    @EntityGraph(attributePaths = "formateurs")
    List<Cours> findAll();

    @EntityGraph(attributePaths = "formateurs")
    List<Cours> findByFormateurs_Id(Long formateurId);
}
