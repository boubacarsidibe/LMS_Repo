package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.AlbumPhoto;

@Repository
public interface AlbumPhotoRepository extends JpaRepository<AlbumPhoto, Long> {

    /** Albums de la promotion de l'étudiant + albums institutionnels. */
    @Query("""
            SELECT a FROM AlbumPhoto a
            WHERE a.promotion IS NULL OR a.promotion.id = :promotionId
            ORDER BY a.dateEvenement DESC NULLS LAST, a.id DESC
            """)
    List<AlbumPhoto> findVisiblesPour(@Param("promotionId") Long promotionId);

    @Query("SELECT a FROM AlbumPhoto a WHERE a.promotion IS NULL ORDER BY a.dateEvenement DESC NULLS LAST, a.id DESC")
    List<AlbumPhoto> findGeneraux();

    boolean existsByTitre(String titre);
}
