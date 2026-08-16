package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.Annonce;

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    /**
     * Annonces visibles par un étudiant : celles de sa promotion et celles
     * diffusées à tout l'institut ({@code promotion IS NULL}).
     */
    @Query("""
            SELECT a FROM Annonce a
            WHERE a.promotion IS NULL OR a.promotion.id = :promotionId
            ORDER BY a.epinglee DESC, a.datePublication DESC
            """)
    List<Annonce> findVisiblesPour(@Param("promotionId") Long promotionId);

    /** Annonces institutionnelles, pour un étudiant sans promotion affectée. */
    @Query("""
            SELECT a FROM Annonce a
            WHERE a.promotion IS NULL
            ORDER BY a.epinglee DESC, a.datePublication DESC
            """)
    List<Annonce> findGenerales();

    List<Annonce> findAllByOrderByEpingleeDescDatePublicationDesc();
}
