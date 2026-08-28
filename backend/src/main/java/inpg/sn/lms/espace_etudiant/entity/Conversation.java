package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Fil de messagerie privée entre deux personnes (apprenant ↔ formateur).
 * Distinct du forum : ici la visibilité est strictement les deux
 * participants, pas un groupe scopé à un cours ou une promotion.
 */
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant1_id", nullable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant2_id", nullable = false)
    private User participant2;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    protected Conversation() {
        // requis par JPA
    }

    public Conversation(User participant1, User participant2) {
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getParticipant1() {
        return participant1;
    }

    public User getParticipant2() {
        return participant2;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    /** L'autre personne de cette conversation, vue depuis {@code utilisateur}. */
    public User autreParticipant(User utilisateur) {
        return participant1.getId().equals(utilisateur.getId()) ? participant2 : participant1;
    }
}
