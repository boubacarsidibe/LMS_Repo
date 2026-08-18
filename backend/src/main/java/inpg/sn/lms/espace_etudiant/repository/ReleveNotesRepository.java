package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.ReleveNotes;

public interface ReleveNotesRepository extends JpaRepository<ReleveNotes, Long> {

    /** Les lignes sont chargées d'emblée : sans elles un relevé n'a rien à montrer. */
    @EntityGraph(attributePaths = { "lignes", "etudiant", "promotion" })
    List<ReleveNotes> findByEtudiant_IdOrderByDateEmissionDesc(Long etudiantId);

    @EntityGraph(attributePaths = { "lignes", "etudiant", "promotion" })
    List<ReleveNotes> findAllByOrderByDateEmissionDesc();
}
