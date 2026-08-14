package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import java.util.List;
import java.util.Optional;

public interface CoursRepository extends JpaRepository<Cours, Long> {

    @Override
    @EntityGraph(attributePaths = {"formateurs", "cohortes"})
    List<Cours> findAll();

    @Override
    @EntityGraph(attributePaths = {"formateurs", "cohortes"})
    Optional<Cours> findById(Long id);

    @EntityGraph(attributePaths = {"formateurs", "cohortes"})
    List<Cours> findByFormateurs_Id(Long formateurId);
}
