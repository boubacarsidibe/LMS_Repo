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
 * Accusé de lecture d'une {@link Annonce} par un étudiant.
 *
 * <p>Entité à part entière plutôt qu'une simple table de jointure : elle porte
 * la date de lecture et permet de compter les non-lues sans charger les annonces.
 */
@Entity
@Table(name = "annonces_lectures", uniqueConstraints = @UniqueConstraint(columnNames = { "annonce_id",
        "etudiant_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnonceLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annonce_id", nullable = false)
    private Annonce annonce;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;

    @Column(nullable = false)
    private LocalDateTime dateLecture = LocalDateTime.now();

    public AnnonceLecture(Annonce annonce, Etudiant etudiant) {
        this.annonce = annonce;
        this.etudiant = etudiant;
        this.dateLecture = LocalDateTime.now();
    }
}
