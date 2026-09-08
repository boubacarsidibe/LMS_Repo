package inpg.sn.lms.recrutementformateur.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.recrutementformateur.enums.EnumDecisionCandidatureFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutCandidatureFormateur;
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
 * Candidature à une offre de recrutement de formateur (QMS-007 §3-4).
 *
 * <p>Suivi en trois états ({@link EnumStatutCandidatureFormateur}) ; la décision
 * ({@link EnumDecisionCandidatureFormateur}) est posée au passage en
 * {@code TERMINE}, avec motif obligatoire pour {@code NON_RETENU}. Les
 * coordonnées sont recopiées (le compte candidat peut disparaître ensuite).
 * {@code formateurCree} garantit qu'un {@code RETENU} ne crée le compte
 * formateur qu'une seule fois.
 */
@Entity
@Table(name = "candidatures_formateur")
@Getter
@Setter
@NoArgsConstructor
public class CandidatureFormateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreRecrutementFormateur offre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidat_user_id", nullable = false)
    private User candidat;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(nullable = false, length = 120)
    private String prenom;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 30)
    private String telephone;

    @Column(name = "diplome_principal", length = 300)
    private String diplomePrincipal;

    @Column(name = "annees_experience")
    private Integer anneesExperience;

    @Column(name = "lettre_motivation", length = 4000)
    private String lettreMotivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnumStatutCandidatureFormateur statut = EnumStatutCandidatureFormateur.DOSSIER_RECU;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EnumDecisionCandidatureFormateur decision;

    @Column(name = "motif_decision", length = 2000)
    private String motifDecision;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission = LocalDateTime.now();

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_user_id")
    private User traitePar;

    @Column(name = "formateur_cree", nullable = false)
    private boolean formateurCree = false;

    @OneToMany(mappedBy = "candidature", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PieceCandidatureFormateur> pieces = new ArrayList<>();

    public CandidatureFormateur(OffreRecrutementFormateur offre, User candidat, String nom, String prenom,
            String email, String telephone) {
        this.offre = offre;
        this.candidat = candidat;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.statut = EnumStatutCandidatureFormateur.DOSSIER_RECU;
        this.dateSoumission = LocalDateTime.now();
    }
}
