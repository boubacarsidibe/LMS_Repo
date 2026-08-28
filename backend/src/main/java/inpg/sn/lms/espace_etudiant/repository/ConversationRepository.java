package inpg.sn.lms.espace_etudiant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import inpg.sn.lms.espace_etudiant.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(attributePaths = { "participant1", "participant2" })
    @Query("select c from Conversation c where (c.participant1.id = :u1 and c.participant2.id = :u2) "
            + "or (c.participant1.id = :u2 and c.participant2.id = :u1)")
    Optional<Conversation> findEntreParticipants(@Param("u1") Long u1, @Param("u2") Long u2);

    @EntityGraph(attributePaths = { "participant1", "participant2" })
    @Query("select c from Conversation c where c.participant1.id = :userId or c.participant2.id = :userId")
    List<Conversation> findDeParticipant(@Param("userId") Long userId);

    @Override
    @EntityGraph(attributePaths = { "participant1", "participant2" })
    Optional<Conversation> findById(Long id);
}
