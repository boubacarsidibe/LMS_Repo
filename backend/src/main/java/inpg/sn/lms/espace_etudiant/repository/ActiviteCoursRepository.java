package inpg.sn.lms.espace_etudiant.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.ActiviteCours;

public interface ActiviteCoursRepository extends JpaRepository<ActiviteCours, Long> {
    @EntityGraph(attributePaths = { "cours", "cours.formateurs", "auteur" })
    List<ActiviteCours> findByCoursIdOrderByDatePublicationDesc(Long coursId);
    @EntityGraph(attributePaths = { "cours", "cours.formateurs", "auteur" })
    List<ActiviteCours> findByCoursIdAndDatePublicationLessThanEqualOrderByDatePublicationDesc(Long coursId, LocalDateTime date);
    @Override @EntityGraph(attributePaths = { "cours", "cours.formateurs", "auteur" })
    java.util.Optional<ActiviteCours> findById(Long id);
}
