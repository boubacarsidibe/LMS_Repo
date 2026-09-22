package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Seance;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    List<Seance> findByCours_Formateurs_IdOrderByDateHeureAsc(Long formateurId);

    /** Calendrier académique d'un étudiant : les séances de ses cours autorisés. */
    List<Seance> findByCoursIdInOrderByDateHeureAsc(List<Long> coursIds);

    /**
     * cours, cours.formateurs et cours.promotions precharges : le mapping vers
     * SeanceDto lit cours.getNom(), le controle de perimetre lit
     * cours.getFormateurs() (PerimetreFormateurService.estFormateurDuCours), et
     * la liste de presence lit cours.getPromotions() pour retrouver les etudiants
     * inscrits. Couvrir cette methode findById specifiquement : c'est l'oubli de
     * findById qui a cause le 5e episode du meme piege sur NoteModuleRepository.
     */
    @Override
    @EntityGraph(attributePaths = { "cours", "cours.formateurs", "cours.formations" })
    Optional<Seance> findById(Long id);

    void deleteByCoursId(Long coursId);

    boolean existsByCours_Formations_Id(Long formationId);
}
