package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Presence;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    @EntityGraph(attributePaths = "etudiant")
    List<Presence> findBySeanceId(Long seanceId);

    @EntityGraph(attributePaths = { "seance", "seance.cours" })
    List<Presence> findByEtudiantIdOrderBySeance_DateHeureDesc(Long etudiantId);

    Optional<Presence> findBySeanceIdAndEtudiantId(Long seanceId, Long etudiantId);

    void deleteBySeance_CoursId(Long coursId);
}
