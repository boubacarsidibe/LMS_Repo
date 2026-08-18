package inpg.sn.lms.espace_etudiant.entity;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Personne citée dans un message du forum.
 *
 * <p>La citation est <strong>stockée</strong>, et non redéduite du texte à
 * l'affichage. Un nom peut s'écrire de plusieurs façons, changer, ou apparaître par
 * hasard dans une phrase : relire le texte pour retrouver les mentions donnerait des
 * résultats différents selon le moment. Ici la citation est un fait, établi une fois à
 * la publication — c'est ce qui permet de dire avec certitude qui a été notifié.
 */
@Entity
@Table(name = "forum_mention")
public class ForumMention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ForumMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User cite;

    protected ForumMention() {
        // requis par JPA
    }

    public ForumMention(ForumMessage message, User cite) {
        this.message = message;
        this.cite = cite;
    }

    public Long getId() {
        return id;
    }

    public ForumMessage getMessage() {
        return message;
    }

    public User getCite() {
        return cite;
    }
}
