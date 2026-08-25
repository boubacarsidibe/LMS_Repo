package inpg.sn.lms.entreprise.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.Entreprise;
import inpg.sn.lms.entreprise.enums.EnumStatutDemandeEntreprise;
import inpg.sn.lms.entreprise.enums.EnumTypeDemandeEntreprise;
import inpg.sn.lms.espace_etudiant.entity.Formation;
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
 * Sollicitation d'une entreprise (TDR §H) : soit un besoin de formation
 * exprimé librement, soit une demande de devis/session sur mesure — les deux
 * partagent la même mécanique de suivi (description, statut, réponse de
 * l'équipe pédagogique), seuls {@code effectifConcerne} et
 * {@code datesSouhaitees} sont propres au devis (laissés {@code null} pour un
 * simple besoin).
 */
@Entity
@Table(name = "demandes_entreprise")
@Getter
@Setter
@NoArgsConstructor
public class DemandeEntreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumTypeDemandeEntreprise type;

    /** Formation visée, si l'entreprise en cible une du catalogue existant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id")
    private Formation formation;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "effectif_concerne")
    private Integer effectifConcerne;

    @Column(name = "dates_souhaitees")
    private String datesSouhaitees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumStatutDemandeEntreprise statut = EnumStatutDemandeEntreprise.SOUMISE;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission = LocalDateTime.now();

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    @Column(name = "reponse_equipe", length = 2000)
    private String reponseEquipe;

    public DemandeEntreprise(Entreprise entreprise, EnumTypeDemandeEntreprise type, Formation formation,
            String description, Integer effectifConcerne, String datesSouhaitees) {
        this.entreprise = entreprise;
        this.type = type;
        this.formation = formation;
        this.description = description;
        this.effectifConcerne = effectifConcerne;
        this.datesSouhaitees = datesSouhaitees;
        this.statut = EnumStatutDemandeEntreprise.SOUMISE;
        this.dateSoumission = LocalDateTime.now();
    }
}
