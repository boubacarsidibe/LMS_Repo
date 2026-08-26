package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.NoteModule;

public interface NoteModuleRepository extends JpaRepository<NoteModule, Long> {

    /**
     * cours et cours.formateurs precharges : le mapping vers NoteModuleDto lit
     * cours.getNom()/getTitre(), et le controle de perimetre a la suppression lit
     * cours.getFormateurs() (PerimetreFormateurService.estFormateurDuCours).
     */
    @EntityGraph(attributePaths = { "cours", "cours.formateurs" })
    List<NoteModule> findByEtudiantIdOrderByDateEvaluationDesc(Long etudiantId);

    @Override
    @EntityGraph(attributePaths = { "cours", "cours.formateurs" })
    Optional<NoteModule> findById(Long id);

    void deleteByCoursId(Long coursId);

    void deleteByEtudiantId(Long etudiantId);
}
