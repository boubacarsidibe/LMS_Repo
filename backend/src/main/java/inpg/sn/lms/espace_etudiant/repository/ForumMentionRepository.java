package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.ForumMention;

public interface ForumMentionRepository extends JpaRepository<ForumMention, Long> {

    /** Les mentions de tout un fil, en une requête plutôt qu'une par message. */
    List<ForumMention> findByMessage_Sujet_Id(Long sujetId);

    void deleteByMessage_Sujet_CoursId(Long coursId);
}
