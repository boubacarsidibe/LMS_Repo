package inpg.sn.lms.recrutementformateur.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;

/**
 * Une formation suivie par le candidat, déclarée dans son dossier (bloc
 * répétable « Formation » du dossier façon SuccessFactors). Patron
 * {@link PieceCandidatureFormateur} : recréée en bloc à chaque dépôt, pas
 * d'endpoint de modification individuelle.
 */
@Entity
@Table(name = "formations_candidature_formateur")
@Getter
@Setter
@NoArgsConstructor
public class FormationCandidatureFormateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false)
    @JsonIgnore
    private CandidatureFormateur candidature;

    @Column(name = "niveau_etude", length = 150)
    private String niveauEtude;

    @Column(length = 100)
    private String pays;

    @Column(length = 120)
    private String ville;

    @Column(length = 200)
    private String etablissement;

    @Column(name = "intitule_diplome", length = 200)
    private String intituleDiplome;

    @Column(name = "matiere_principale", length = 200)
    private String matierePrincipale;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_obtention")
    private LocalDate dateObtention;

    public FormationCandidatureFormateur(CandidatureFormateur candidature, String niveauEtude, String pays,
            String ville, String etablissement, String intituleDiplome, String matierePrincipale,
            LocalDate dateDebut, LocalDate dateObtention) {
        this.candidature = candidature;
        this.niveauEtude = niveauEtude;
        this.pays = pays;
        this.ville = ville;
        this.etablissement = etablissement;
        this.intituleDiplome = intituleDiplome;
        this.matierePrincipale = matierePrincipale;
        this.dateDebut = dateDebut;
        this.dateObtention = dateObtention;
    }
}
