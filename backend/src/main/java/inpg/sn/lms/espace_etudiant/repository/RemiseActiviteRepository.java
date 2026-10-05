package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import inpg.sn.lms.espace_etudiant.entity.RemiseActivite;

public interface RemiseActiviteRepository extends JpaRepository<RemiseActivite, Long> {
    @EntityGraph(attributePaths = { "etudiant", "etudiant.user", "activite", "activite.cours" })
    List<RemiseActivite> findByActiviteIdOrderByRemiseLeDesc(Long activiteId);
    @EntityGraph(attributePaths = { "etudiant", "etudiant.user", "activite", "activite.cours" })
    Optional<RemiseActivite> findByActiviteIdAndEtudiantId(Long activiteId, Long etudiantId);
    @Override @EntityGraph(attributePaths = { "etudiant", "etudiant.user", "activite", "activite.cours", "activite.cours.formateurs" })
    Optional<RemiseActivite> findById(Long id);
}
