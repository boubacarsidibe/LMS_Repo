package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sujet du forum de discussion (TDR §3.2).
 *
 * <p>Un sujet peut être rattaché à un {@link Cours} — il n'est alors visible que
 * des étudiants dont la promotion suit ce cours — ou à une {@link Promotion} — il
 * n'est alors visible que des étudiants de cette promotion, quel que soit leur
 * cours — ou rester général ({@code cours == null} et {@code promotion == null}),
 * auquel cas sa {@link #portee} décide qui le voit : tout l'espace étudiant, ou
 * l'espace alumni (TDR §C, migration V13).
 *
 * <p>{@code cours} et {@code promotion} sont mutuellement exclusifs sur un même
 * sujet — jamais les deux rattachements à la fois, pour ne pas reproduire la
 * confusion entre deux chaînes concurrentes déjà rencontrée par le passé
 * (CLAUDE.md §3, {@code cours_promotion}).
 */
@Entity
@Table(name = "forum_sujets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForumSujet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    /** Cours de rattachement, ou {@code null} pour un salon général ou de promotion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cours_id")
    private Cours cours;

    /** Promotion de rattachement, ou {@code null} pour un salon général ou de cours. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id")
    private User auteur;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(nullable = false)
    private boolean epingle = false;

    /** Un sujet clos reste consultable mais n'accepte plus de réponse. */
    @Column(nullable = false)
    private boolean clos = false;

    /** N'a de sens que pour un sujet général ({@code cours == null}), voir la classe. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PorteeForumSujet portee = PorteeForumSujet.ETUDIANT;

    @OneToMany(mappedBy = "sujet", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ForumMessage> messages = new ArrayList<>();

    public ForumSujet(String titre, Cours cours, User auteur) {
        this.titre = titre;
        this.cours = cours;
        this.auteur = auteur;
        this.dateCreation = LocalDateTime.now();
        this.portee = PorteeForumSujet.ETUDIANT;
    }

    public ForumSujet(String titre, User auteur, PorteeForumSujet portee) {
        this.titre = titre;
        this.cours = null;
        this.auteur = auteur;
        this.dateCreation = LocalDateTime.now();
        this.portee = portee;
    }

    /** Sujet scopé à une promotion : visible des seuls étudiants qui en font partie. */
    public ForumSujet(String titre, Promotion promotion, User auteur) {
        this.titre = titre;
        this.promotion = promotion;
        this.auteur = auteur;
        this.dateCreation = LocalDateTime.now();
        this.portee = PorteeForumSujet.ETUDIANT;
    }
}
