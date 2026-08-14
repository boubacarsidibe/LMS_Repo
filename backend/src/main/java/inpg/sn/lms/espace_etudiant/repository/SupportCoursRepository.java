package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import inpg.sn.lms.espace_etudiant.dto.CompteurParId;
import inpg.sn.lms.espace_etudiant.entity.SupportCours;

public interface SupportCoursRepository extends JpaRepository<SupportCours, Long> {

    /**
     * Les controles de visibilite verifient si l'appelant est formateur du cours
     * (cours.formateurs) ET si son cours partage une cohorte avec l'appelant
     * (cours.cohortes) : les deux doivent etre precharges, sinon
     * LazyInitializationException (le projet tourne avec open-in-view=false).
     * Pas de risque de MultipleBagFetchException : formateurs est une List mais
     * cohortes est un Set (meme pattern deja utilise dans CoursRepository).
     */
    @Override
    @EntityGraph(attributePaths = { "cours", "cours.formateurs", "cours.cohortes" })
    List<SupportCours> findAll();

    /** Meme precharge que findAll. */
    @Override
    @EntityGraph(attributePaths = { "cours", "cours.formateurs", "cours.cohortes" })
    Optional<SupportCours> findById(Long id);

    long countByCoursId(Long coursId);

    /**
     * Compte les supports de plusieurs cours en UNE seule requete.
     * Les cours sans support sont absents du resultat : utiliser getOrDefault(id, 0L).
     */
    @Query("SELECT new inpg.sn.lms.espace_etudiant.dto.CompteurParId(s.cours.id, COUNT(s)) "
            + "FROM SupportCours s WHERE s.cours.id IN :coursIds GROUP BY s.cours.id")
    List<CompteurParId> compterParCours(@Param("coursIds") List<Long> coursIds);
}
