package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import inpg.sn.lms.espace_etudiant.entity.MessagePrive;

public interface MessagePriveRepository extends JpaRepository<MessagePrive, Long> {

    @EntityGraph(attributePaths = "auteur")
    List<MessagePrive> findByConversationIdOrderByDateEnvoiAsc(Long conversationId);

    MessagePrive findFirstByConversationIdOrderByDateEnvoiDesc(Long conversationId);

    long countByConversationIdAndAuteurIdNotAndLuFalse(Long conversationId, Long lecteurId);

    List<MessagePrive> findByConversationIdAndAuteurIdNotAndLuFalse(Long conversationId, Long lecteurId);
}
