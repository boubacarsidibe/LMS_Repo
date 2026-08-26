package inpg.sn.lms.accreditation.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.accreditation.enums.EnumAvisAccreditation;
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
 * Avis d'un évaluateur sur une demande d'accréditation (TDR §F : « ils
 * puissent... rendre leurs décisions », au pluriel).
 *
 * <p>Un avis par (demande, évaluateur) — contrainte unique en base (migration
 * V20). Redéposer met à jour l'avis existant plutôt que d'en empiler un
 * second, même patron que {@code retour_evaluation} (V7). La décision finale
 * (accréditer/rejeter) reste un acte explicite, distinct de ces avis : elle
 * exige qu'au moins un avis existe avant de trancher, sans imposer de règle
 * de majorité que le TDR ne spécifie pas.
 */
@Entity
@Table(name = "avis_accreditation")
@Getter
@Setter
@NoArgsConstructor
public class AvisAccreditation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private DemandeAccreditation demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id", nullable = false)
    private User evaluateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumAvisAccreditation avis;

    @Column(length = 1000)
    private String commentaire;

    @Column(name = "date_avis", nullable = false)
    private LocalDateTime dateAvis = LocalDateTime.now();

    public AvisAccreditation(DemandeAccreditation demande, User evaluateur, EnumAvisAccreditation avis,
            String commentaire) {
        this.demande = demande;
        this.evaluateur = evaluateur;
        this.avis = avis;
        this.commentaire = commentaire;
        this.dateAvis = LocalDateTime.now();
    }
}
