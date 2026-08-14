package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import inpg.sn.lms.espace_etudiant.entity.ForumMessage;

@Repository
public interface ForumMessageRepository extends JpaRepository<ForumMessage, Long> {

    List<ForumMessage> findBySujetIdOrderByDateCreationAsc(Long sujetId);

    long countBySujetId(Long sujetId);
}
