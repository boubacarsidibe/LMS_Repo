package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.AnnonceLecture;

@Repository
public interface AnnonceLectureRepository extends JpaRepository<AnnonceLecture, Long> {

    boolean existsByAnnonceIdAndEtudiantId(Long annonceId, Long etudiantId);

    /**
     * Identifiants des annonces déjà lues par l'étudiant. Renvoyer les seuls ids
     * évite de charger les entités juste pour calculer un état « lu / non lu ».
     */
    @Query("SELECT l.annonce.id FROM AnnonceLecture l WHERE l.etudiant.id = :etudiantId")
    List<Long> findAnnonceIdsLues(@Param("etudiantId") Long etudiantId);
}
