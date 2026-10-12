package inpg.sn.lms.recrutementformateur.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.User;
import inpg.sn.lms.espace_etudiant.entity.Formation;
import inpg.sn.lms.recrutement.entity.FichierStocke;
import inpg.sn.lms.recrutementformateur.enums.EnumStatutOffreFormateur;
import inpg.sn.lms.recrutementformateur.enums.EnumTypeContratFormateur;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fiche de poste d'un recrutement de formateur (procédure QMS-007 §1).
 *
 * <p>Une offre naît en {@code BROUILLON}, invisible du public tant qu'elle n'est
 * pas {@code PUBLIEE}. {@code formation} est facultative : elle rattache l'offre
 * à un programme précis, ce qui permettra plus tard d'affecter le formateur
 * retenu à ses cours ({@code cours_formateur}).
 */
@Entity
@Table(name = "offres_recrutement_formateur")
@Getter
@Setter
@NoArgsConstructor
public class OffreRecrutementFormateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String intitule;

    @Column(length = 120)
    private String domaine;

    @Column(name = "description_missions", length = 4000)
    private String descriptionMissions;

    @Column(name = "profil_recherche", length = 4000)
    private String profilRecherche;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 20)
    private EnumTypeContratFormateur typeContrat;

    @Column(length = 200)
    private String lieu;

    @Column(name = "nb_postes", nullable = false)
    private int nbPostes = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id")
    private Formation formation;

    @Column(name = "date_publication")
    private LocalDate datePublication;

    @Column(name = "date_limite_candidature")
    private LocalDate dateLimiteCandidature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnumStatutOffreFormateur statut = EnumStatutOffreFormateur.BROUILLON;

    /**
     * Fiche de poste officielle (PDF), facultative. Volontairement sans {@code cascade} ni
     * {@code orphanRemoval} : le fichier se retire par {@code FichierStockeService}, qui supprime
     * aussi le fichier sur disque une fois la transaction validée.
     */
    @OneToOne
    @JoinColumn(name = "fiche_poste_id")
    private FichierStocke fichePoste;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_user_id")
    private User creePar;
}
