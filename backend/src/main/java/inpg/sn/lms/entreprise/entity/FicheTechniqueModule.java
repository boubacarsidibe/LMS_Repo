package inpg.sn.lms.entreprise.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.entreprise.enums.EnumStatutFicheModule;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fiche Technique de Module : livrable structuré que le responsable pédagogique
 * (DPI) produit pour une demande d'entreprise « devis / session sur mesure »
 * (TDR §H, procédures QMS-006 / QMS-008 / QMS-010). Reprend les 10 sections du
 * gabarit officiel de l'INPG ; le planning jour par jour (§4) est porté par
 * {@link FicheModuleJour}, les autres sections restent du texte.
 *
 * <p>Une fiche par demande (contrainte d'unicité en base, migration V33).
 */
@Entity
@Table(name = "fiche_technique_module")
@Getter
@Setter
@NoArgsConstructor
public class FicheTechniqueModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false, unique = true)
    @JsonIgnore
    private DemandeEntreprise demande;

    // ===== En-tête =====
    @Column(length = 60)
    private String code;
    @Column(length = 300)
    private String intitule;
    @Column(length = 30)
    private String version;
    @Column(name = "date_revision")
    private LocalDate dateRevision;
    @Column(name = "responsable_module", length = 200)
    private String responsableModule;

    // ===== §1 Caractéristiques des cibles à former =====
    @Column(length = 200)
    private String departement;
    @Column(name = "atelier_service", length = 200)
    private String atelierService;
    @Column(name = "fonctions_cibles", length = 500)
    private String fonctionsCibles;
    @Column(length = 500)
    private String thematiques;
    @Column(length = 1000)
    private String prerequis;
    @Column(length = 60)
    private String langue = "Français";
    @Column(length = 120)
    private String niveau;
    private Integer effectif;
    @Column(name = "duree_heures")
    private Integer dureeHeures;
    @Column(name = "duree_jours")
    private Integer dureeJours;
    @Column(name = "periode_probable", length = 120)
    private String periodeProbable;

    // ===== §2 Objectifs d'apprentissage =====
    @Column(name = "objectif_general", length = 2000)
    private String objectifGeneral;
    @Column(name = "objectifs_specifiques", length = 4000)
    private String objectifsSpecifiques;

    // ===== §3 Modalités de test de prérequis =====
    @Column(name = "modalites_test_prerequis", length = 2000)
    private String modalitesTestPrerequis;

    // ===== §5 Formation sur site =====
    @Column(name = "formation_sur_site", length = 1000)
    private String formationSurSite;

    // ===== §6 Types d'évaluation =====
    @Column(name = "types_evaluation", length = 2000)
    private String typesEvaluation;

    // ===== §7 Ressources pédagogiques =====
    @Column(name = "ressources_pedagogiques", length = 2000)
    private String ressourcesPedagogiques;

    // ===== §8 Bibliographie =====
    @Column(length = 2000)
    private String bibliographie;

    // ===== §9 Profil des formateurs =====
    @Column(name = "profil_formateur", length = 2000)
    private String profilFormateur;

    // ===== Proposition financière (QMS-008) =====
    @Column(name = "cout_total", length = 120)
    private String coutTotal;
    @Column(name = "modalites_financieres", length = 1000)
    private String modalitesFinancieres;

    // ===== §10 Cycle de vie =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnumStatutFicheModule statut = EnumStatutFicheModule.BROUILLON;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_transmission")
    private LocalDateTime dateTransmission;

    @Column(name = "commentaire_entreprise", length = 2000)
    private String commentaireEntreprise;

    @Column(name = "date_decision_entreprise")
    private LocalDateTime dateDecisionEntreprise;

    @OneToMany(mappedBy = "fiche", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<FicheModuleJour> jours = new ArrayList<>();

    public FicheTechniqueModule(DemandeEntreprise demande) {
        this.demande = demande;
        this.statut = EnumStatutFicheModule.BROUILLON;
        this.dateCreation = LocalDateTime.now();
    }
}
