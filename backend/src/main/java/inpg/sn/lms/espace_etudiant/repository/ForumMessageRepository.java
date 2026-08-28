package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.ForumMessage;
import inpg.sn.lms.espace_etudiant.entity.PorteeForumSujet;

@Repository
public interface ForumMessageRepository extends JpaRepository<ForumMessage, Long> {

    List<ForumMessage> findBySujetIdOrderByDateCreationAsc(Long sujetId);

    long countBySujetId(Long sujetId);

    void deleteBySujet_CoursId(Long coursId);

    void deleteBySujet_PromotionId(Long promotionId);

    /** Sert à bloquer la suppression d'un compte ayant publié dans le forum. */
    boolean existsByAuteurId(Long auteurId);

    /** Messages postés par un alumni dans le salon qui lui est propre — profil enrichi. */
    long countByAuteurIdAndSujet_Portee(Long auteurId, PorteeForumSujet portee);
}
