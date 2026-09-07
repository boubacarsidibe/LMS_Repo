package inpg.sn.lms.accreditation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.accreditation.enums.EnumNiveauProgramme;
import inpg.sn.lms.accreditation.enums.EnumStatutDemandeAccreditation;
import inpg.sn.lms.accreditation.enums.EnumTypeStructure;
import inpg.sn.lms.auth.entity.EcoleFormation;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Demande d'accréditation d'une école de formation (TDR §F).
 *
 * <p>Une école ne peut avoir qu'une seule demande active (SOUMISE ou
 * EN_COURS_EVALUATION) à la fois — appliqué par un index partiel en base
 * (migration V15), pas seulement en Java : un contrôle uniquement applicatif
 * laisserait passer deux requêtes concurrentes.
 */
@Entity
@Table(name = "demandes_accreditation")
@Getter
@Setter
@NoArgsConstructor
public class DemandeAccreditation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecole_id", nullable = false)
    private EcoleFormation ecole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnumStatutDemandeAccreditation statut = EnumStatutDemandeAccreditation.SOUMISE;

    // ===== Programme visé (Cadre de référence DRCQ §II : « l'accréditation
    // concerne les programmes de formation, non l'établissement ») =====

    @Column(name = "intitule_programme", length = 200)
    private String intituleProgramme;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_programme", length = 30)
    private EnumNiveauProgramme niveauProgramme;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_structure", length = 40)
    private EnumTypeStructure typeStructure;

    /** Effectif de la cohorte concernée par la demande. */
    @Column(name = "effectif_cohorte")
    private Integer effectifCohorte;

    @Column(name = "date_debut_cohorte")
    private LocalDate dateDebutCohorte;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission = LocalDateTime.now();

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    /**
     * Fin de validité de l'accréditation : décision + 3 ans (Cadre §V.4).
     * Renseignée uniquement pour ACCREDITE et ACCREDITE_SOUS_RESERVE.
     */
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "motif_rejet", length = 1000)
    private String motifRejet;

    @Column(name = "commentaire_evaluation", length = 1000)
    private String commentaireEvaluation;

    /**
     * Plan d'actions d'une accréditation conditionnelle (Cadre §IV.8) :
     * mesures que l'établissement doit mettre en œuvre. Obligatoire pour
     * ACCREDITE_SOUS_RESERVE, sinon nul.
     */
    @Column(name = "plan_actions", length = 2000)
    private String planActions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id")
    private User evaluateur;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PieceAccreditation> pieces = new ArrayList<>();

    public DemandeAccreditation(EcoleFormation ecole) {
        this.ecole = ecole;
        this.statut = EnumStatutDemandeAccreditation.SOUMISE;
        this.dateSoumission = LocalDateTime.now();
    }
}
