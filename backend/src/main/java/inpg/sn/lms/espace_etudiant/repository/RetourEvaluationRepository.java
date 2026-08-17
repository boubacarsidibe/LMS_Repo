package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import inpg.sn.lms.espace_etudiant.entity.RetourEvaluation;

/**
 * Avis des étudiants sur les cours.
 *
 * <p>Table unique depuis la fusion : {@code evaluations_cours} recueillait le dépôt
 * de l'étudiant tandis que celle-ci alimentait l'écran du formateur, si bien qu'un
 * avis déposé n'atteignait jamais son destinataire.
 */
public interface RetourEvaluationRepository extends JpaRepository<RetourEvaluation, Long> {

    /**
     * Les avis épinglés remontent en tête, puis les plus récents.
     *
     * <p>Sans tri explicite, la base renvoyait les lignes dans un ordre arbitraire :
     * épingler un avis ne produisait alors aucun effet visible, et l'onglet
     * « Plus récents » de l'écran formateur n'était pas chronologique.
     */
    @EntityGraph(attributePaths = "cours")
    List<RetourEvaluation> findByCours_IdInOrderByEpingleDescDateCreationDesc(List<Long> coursIds);

    @Override
    @EntityGraph(attributePaths = {"cours", "cours.formateurs"})
    Optional<RetourEvaluation> findById(Long id);

    /** L'avis de cet étudiant sur ce cours : il n'y en a qu'un, redéposer le met à jour. */
    @EntityGraph(attributePaths = "cours")
    Optional<RetourEvaluation> findByEtudiantIdAndCoursId(Long etudiantId, Long coursId);

    /** Les avis déposés par un étudiant, du plus récent au plus ancien. */
    @EntityGraph(attributePaths = "cours")
    List<RetourEvaluation> findByEtudiantIdOrderByDateCreationDesc(Long etudiantId);

    /** Tous les avis, cours precharge : sert au suivi qualite de l'etablissement. */
    @EntityGraph(attributePaths = "cours")
    List<RetourEvaluation> findAllByOrderByDateCreationDesc();

    long countByCoursId(Long coursId);

    /** Note moyenne du cours, {@code null} tant qu'aucun avis n'a été déposé. */
    @Query("SELECT AVG(r.note) FROM RetourEvaluation r WHERE r.cours.id = :coursId")
    Double moyenneParCours(@Param("coursId") Long coursId);
}
