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
 * Trace de consultation d'un {@link SupportCours} par un étudiant.
 *
 * <p>Support du « suivi des apprentissages » du §3.5 : la progression d'un cours
 * n'est jamais stockée, elle se calcule à la volée comme le rapport entre le
 * nombre de supports consultés et le nombre total de supports du cours.
 */
@Entity
@Table(name = "progressions_support", uniqueConstraints = @UniqueConstraint(columnNames = { "etudiant_id",
        "support_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressionSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "support_id", nullable = false)
    private SupportCours support;

    @Column(nullable = false)
    private LocalDateTime dateConsultation = LocalDateTime.now();

    public ProgressionSupport(Etudiant etudiant, SupportCours support) {
        this.etudiant = etudiant;
        this.support = support;
        this.dateConsultation = LocalDateTime.now();
    }
}
