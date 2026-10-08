package inpg.sn.lms.espace_etudiant.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.ForumLecture;

@Repository
public interface ForumLectureRepository extends JpaRepository<ForumLecture, Long> {

    /** Tous les passages d'un utilisateur : une requête pour toute la liste des sujets. */
    List<ForumLecture> findByUserId(Long userId);

    /**
     * Note que l'utilisateur vient d'ouvrir ce fil.
     *
     * <p>Un upsert natif plutôt qu'un « chercher puis enregistrer » : deux requêtes
     * simultanées du même utilisateur (onglet ouvert deux fois, rafraîchissement du
     * fil) feraient sinon échouer la seconde sur la contrainte d'unicité, et l'écran
     * de lecture renverrait 500 pour une simple pastille.
     *
     * <p>L'instant vient de l'application, pas de {@code now()} côté base : la date
     * des messages ({@code ForumMessage#dateCreation}) est posée par la JVM, et les
     * deux horloges doivent être celles qu'on compare.
     */
    @Modifying
    @Query(value = """
            INSERT INTO forum_lecture (user_id, sujet_id, derniere_lecture)
            VALUES (:userId, :sujetId, :instant)
            ON CONFLICT (user_id, sujet_id) DO UPDATE SET derniere_lecture = EXCLUDED.derniere_lecture
            """, nativeQuery = true)
    void marquerLu(@Param("userId") Long userId, @Param("sujetId") Long sujetId,
            @Param("instant") LocalDateTime instant);
}
