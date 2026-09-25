package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.ForumSujet;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;

@Repository
public interface ForumSujetRepository extends JpaRepository<ForumSujet, Long> {

    /**
     * Sujets accessibles à un étudiant : les salons généraux de l'espace étudiant,
     * ceux rattachés à un cours de son cursus, et ceux rattachés à sa promotion.
     * Les sujets généraux de l'espace alumni ({@code portee = ALUMNI}) sont exclus
     * (voir migration V13).
     */
    @Query("""
            SELECT s FROM ForumSujet s
            WHERE (s.cours IS NULL AND s.promotion IS NULL
                   AND s.portee = inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet.ETUDIANT)
               OR s.cours.id IN :coursIds
               OR (:promotionId IS NOT NULL AND s.promotion.id = :promotionId)
            ORDER BY s.epingle DESC, s.dateCreation DESC
            """)
    List<ForumSujet> findVisiblesPour(@Param("coursIds") List<Long> coursIds, @Param("promotionId") Long promotionId);

    /** Repli lorsque l'étudiant n'a encore aucun cours autorisé. */
    @Query("""
            SELECT s FROM ForumSujet s
            WHERE (s.cours IS NULL AND s.promotion IS NULL
                   AND s.portee = inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet.ETUDIANT)
               OR (:promotionId IS NOT NULL AND s.cours IS NULL AND s.promotion.id = :promotionId)
            ORDER BY s.epingle DESC, s.dateCreation DESC
            """)
    List<ForumSujet> findGeneraux(@Param("promotionId") Long promotionId);

    /**
     * Sujets accessibles à un formateur : les salons généraux de l'espace étudiant,
     * ceux rattachés à un cours qu'il enseigne, et ceux rattachés à une promotion
     * desservie par ses cours (cf. {@code PerimetreFormateurService#cohorteIdsDuFormateur}).
     * Remplace une ancienne vue « formateur/personnel » qui renvoyait l'intégralité
     * des sujets de l'espace étudiant sans tenir compte de {@code cours_formateur} —
     * un formateur sans le moindre cours assigné voyait pourtant tous les fils de
     * tous les cours, à rebours du cloisonnement appliqué partout ailleurs dans cet
     * espace (supports, séances, notes).
     */
    @Query("""
            SELECT s FROM ForumSujet s
            WHERE (s.cours IS NULL AND s.promotion IS NULL
                   AND s.portee = inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet.ETUDIANT)
               OR s.cours.id IN :coursIds
               OR s.promotion.id IN :promotionIds
            ORDER BY s.epingle DESC, s.dateCreation DESC
            """)
    List<ForumSujet> findVisiblesPourFormateur(@Param("coursIds") List<Long> coursIds,
            @Param("promotionIds") List<Long> promotionIds);

    /** Sujets généraux de l'espace alumni, seul salon qui leur soit ouvert. */
    List<ForumSujet> findByPorteeOrderByEpingleDescDateCreationDesc(PorteeForumSujet portee);

    List<ForumSujet> findByCoursIdOrderByEpingleDescDateCreationDesc(Long coursId);

    List<ForumSujet> findByPromotionIdOrderByEpingleDescDateCreationDesc(Long promotionId);

    void deleteByCoursId(Long coursId);

    void deleteByPromotionId(Long promotionId);

    /** Sert à bloquer la suppression d'un compte ayant publié dans le forum. */
    boolean existsByAuteurId(Long auteurId);

    /** Sujets ouverts par un alumni dans le salon qui lui est propre — profil enrichi. */
    long countByAuteurIdAndPortee(Long auteurId, PorteeForumSujet portee);
}
