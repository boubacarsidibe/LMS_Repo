package inpg.sn.lms.rendezvous.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.rendezvous.enums.EnumModaliteRendezVous;
import inpg.sn.lms.rendezvous.enums.EnumStatutRendezVous;
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
 * Demande de rendez-vous déposée sur un {@link CreneauDisponibilite} (TDR
 * §J). L'auteur ({@code demandeur}) est toujours dérivé du jeton, jamais du
 * corps de la requête — même principe que partout ailleurs dans ce dépôt.
 */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creneau_id", nullable = false)
    private CreneauDisponibilite creneau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private User demandeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumModaliteRendezVous modalite;

    @Column(name = "lieu_ou_lien", length = 300)
    private String lieuOuLien;

    @Column(length = 1000)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnumStatutRendezVous statut = EnumStatutRendezVous.EN_ATTENTE;

    /** Nouveau créneau suggéré par le personnel quand il ne peut pas honorer le créneau demandé. */
    @Column(name = "date_heure_proposee_debut")
    private LocalDateTime dateHeureProposeeDebut;

    @Column(name = "date_heure_proposee_fin")
    private LocalDateTime dateHeureProposeeFin;

    @Column(name = "commentaire_reponse", length = 1000)
    private String commentaireReponse;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    public RendezVous(CreneauDisponibilite creneau, User demandeur, EnumModaliteRendezVous modalite,
            String lieuOuLien, String motif) {
        this.creneau = creneau;
        this.demandeur = demandeur;
        this.modalite = modalite;
        this.lieuOuLien = lieuOuLien;
        this.motif = motif;
    }
}
