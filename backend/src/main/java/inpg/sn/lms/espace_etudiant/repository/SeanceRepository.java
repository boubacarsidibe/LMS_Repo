package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Seance;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    /**
     * cours, cours.formateurs et cours.cohortes precharges : le mapping vers
     * SeanceDto lit cours.getNom(), le controle de perimetre lit
     * cours.getFormateurs() (PerimetreFormateurService.estFormateurDuCours), et
     * la liste de presence lit cours.getCohortes() pour retrouver les etudiants
     * inscrits. Couvrir les deux methodes (liste ET findById) : c'est l'oubli de
     * findById qui a cause le 5e episode du meme piege sur NoteModuleRepository.
     */
    @EntityGraph(attributePaths = { "cours", "cours.formateurs", "cours.cohortes" })
    List<Seance> findByCoursIdOrderByDateDesc(Long coursId);

    @Override
    @EntityGraph(attributePaths = { "cours", "cours.formateurs", "cours.cohortes" })
    Optional<Seance> findById(Long id);
}
