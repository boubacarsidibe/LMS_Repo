package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import inpg.sn.lms.espace_etudiant.entity.Cours;
import java.util.List;
import java.util.Optional;

public interface CoursRepository extends JpaRepository<Cours, Long> {

    @Override
    @EntityGraph(attributePaths = {"formateurs", "formations"})
    List<Cours> findAll();

    @Override
    @EntityGraph(attributePaths = {"formateurs", "formations"})
    Optional<Cours> findById(Long id);

    @EntityGraph(attributePaths = {"formateurs", "formations"})
    List<Cours> findByFormateurs_Id(Long formateurId);

    /**
     * Cours au programme d'une formation — c'est la requête qui matérialise les
     * « cours autorisés » d'un étudiant, via sa promotion puis sa formation.
     */
    @EntityGraph(attributePaths = "formateurs")
    List<Cours> findByFormations_Id(Long formationId);
}
