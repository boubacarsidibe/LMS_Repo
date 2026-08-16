package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.auth.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * des étudiants dont la promotion suit ce cours — ou rester général
 * ({@code cours == null}), auquel cas il est ouvert à tout l'espace étudiant.
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

    /** Cours de rattachement, ou {@code null} pour un salon général. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cours_id")
    private Cours cours;

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

    @OneToMany(mappedBy = "sujet", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ForumMessage> messages = new ArrayList<>();

    public ForumSujet(String titre, Cours cours, User auteur) {
        this.titre = titre;
        this.cours = cours;
        this.auteur = auteur;
        this.dateCreation = LocalDateTime.now();
    }
}
