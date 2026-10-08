package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dernier passage d'un utilisateur dans un fil du forum (migration
 * {@code V202610081030}). Sert à compter les messages non lus : ceux postés par un
 * autre après {@link #derniereLecture}.
 *
 * <p>Les identifiants sont de simples colonnes, sans association : cette ligne se
 * lit et s'écrit par {@code ForumLectureRepository}, jamais en parcourant le graphe
 * d'objets, et la suppression d'un sujet ou d'un compte la balaie côté base
 * ({@code ON DELETE CASCADE}).
 */
@Entity
@Table(name = "forum_lecture")
@Getter
@Setter
@NoArgsConstructor
public class ForumLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sujet_id", nullable = false)
    private Long sujetId;

    @Column(name = "derniere_lecture", nullable = false)
    private LocalDateTime derniereLecture;
}
