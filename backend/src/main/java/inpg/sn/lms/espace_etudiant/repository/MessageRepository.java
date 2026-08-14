package inpg.sn.lms.espace_etudiant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
