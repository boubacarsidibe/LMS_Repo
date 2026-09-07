package inpg.sn.lms.accreditation.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.accreditation.enums.EnumAppreciationCritere;
import inpg.sn.lms.auth.entity.User;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Jugement d'un évaluateur sur un critère du référentiel, pour un dossier donné
 * (TDR §F : « définir et gérer les critères d'accréditation », appliqués dossier
 * par dossier). Migration V30.
 *
 * <p>Une entrée par (demande, critère, évaluateur) — contrainte unique en base.
 * Redéposer met à jour l'entrée existante plutôt que d'en empiler une seconde,
 * même patron que {@code AvisAccreditation} (V20) et {@code RetourEvaluation}
 * (V7).
 */
@Entity
@Table(name = "evaluation_critere")
@Getter
@Setter
@NoArgsConstructor
public class EvaluationCritere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeAccreditation demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "critere_id", nullable = false)
    private CritereAccreditation critere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id", nullable = false)
    private User evaluateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumAppreciationCritere appreciation;

    @Column(length = 1000)
    private String commentaire;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDateTime dateEvaluation = LocalDateTime.now();

    public EvaluationCritere(DemandeAccreditation demande, CritereAccreditation critere, User evaluateur,
            EnumAppreciationCritere appreciation, String commentaire) {
        this.demande = demande;
        this.critere = critere;
        this.evaluateur = evaluateur;
        this.appreciation = appreciation;
        this.commentaire = commentaire;
        this.dateEvaluation = LocalDateTime.now();
    }
}
