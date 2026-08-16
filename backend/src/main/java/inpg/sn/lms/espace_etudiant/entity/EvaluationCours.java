package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Etudiant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Évaluation de satisfaction d'un cours par un étudiant (TDR §3.7 « Processus
 * qualité » : « remplir des évaluations de satisfaction à la fin de chaque
 * session », avec notes et commentaires).
 *
 * <p>La contrainte d'unicité (etudiant, cours) garantit une seule évaluation par
 * étudiant et par cours ; une nouvelle soumission met à jour la précédente.
 */
@Entity
@Table(name = "evaluations_cours", uniqueConstraints = @UniqueConstraint(columnNames = { "etudiant_id", "cours_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cours_id", nullable = false)
    private Cours cours;

    /** Note de satisfaction de 1 à 5. */
    @Column(nullable = false)
    private int note;

    @Column(columnDefinition = "text")
    private String commentaire;

    @Column(nullable = false)
    private LocalDateTime dateSoumission = LocalDateTime.now();

    public EvaluationCours(Etudiant etudiant, Cours cours, int note, String commentaire) {
        this.etudiant = etudiant;
        this.cours = cours;
        this.note = note;
        this.commentaire = commentaire;
        this.dateSoumission = LocalDateTime.now();
    }
}
