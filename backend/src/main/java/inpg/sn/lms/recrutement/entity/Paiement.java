package inpg.sn.lms.recrutement.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "paiements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String referenceTransaction;

    @Column(nullable = false)
    private BigDecimal montant;

    @Column(nullable = false)
    private String devise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumStatutPaiement statut;

    @Column
    private LocalDateTime datePaiement;

    /**
     * Moyen de paiement choisi par le candidat (ex. « Wave », « Carte Bancaire »).
     * Persisté pour que le reçu reste fidèle quand on le régénère : il ne peut pas
     * être reconstitué depuis l'état du formulaire une fois la page quittée.
     * Null pour les paiements antérieurs à l'ajout de la colonne.
     */
    @Column
    private String methodePaiement;

    @OneToOne
    @JoinColumn(name = "candidature_id", unique = true)
    private Candidature candidature;

    public boolean effectuerPaiement() {
        if (this.statut == EnumStatutPaiement.EN_ATTENTE) {
            this.statut = EnumStatutPaiement.SUCCES;
            this.datePaiement = LocalDateTime.now();
            return true;
        }
        return false;
    }
}