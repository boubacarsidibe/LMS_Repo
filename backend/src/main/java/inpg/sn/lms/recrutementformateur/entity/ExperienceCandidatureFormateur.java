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
 * Une expérience professionnelle déclarée dans le dossier, avec ses
 * coordonnées de référence (supérieur hiérarchique) — reprend le bloc
 * « Employment History » du portail de référence. Patron
 * {@link PieceCandidatureFormateur} : recréée en bloc à chaque dépôt.
 */
@Entity
@Table(name = "experiences_candidature_formateur")
@Getter
@Setter
@NoArgsConstructor
public class ExperienceCandidatureFormateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false)
    @JsonIgnore
    private CandidatureFormateur candidature;

    @Column(length = 200)
    private String employeur;

    @Column(length = 100)
    private String pays;

    @Column(length = 120)
    private String ville;

    @Column(name = "poste_actuel", nullable = false)
    private boolean posteActuel = false;

    @Column(name = "intitule_poste", length = 200)
    private String intitulePoste;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "nom_superviseur", length = 200)
    private String nomSuperviseur;

    @Column(name = "titre_superviseur", length = 200)
    private String titreSuperviseur;

    @Column(name = "telephone_superviseur", length = 30)
    private String telephoneSuperviseur;

    @Column(name = "email_superviseur", length = 200)
    private String emailSuperviseur;

    /** {@code null} = non renseigné (le candidat n'a pas répondu à la question). */
    @Column(name = "contact_autorise")
    private Boolean contactAutorise;

    @Column(name = "motif_non_contact", length = 500)
    private String motifNonContact;

    @Column(name = "motif_depart", length = 500)
    private String motifDepart;

    public ExperienceCandidatureFormateur(CandidatureFormateur candidature, String employeur, String pays,
            String ville, boolean posteActuel, String intitulePoste, LocalDate dateDebut, LocalDate dateFin,
            String nomSuperviseur, String titreSuperviseur, String telephoneSuperviseur, String emailSuperviseur,
            Boolean contactAutorise, String motifNonContact, String motifDepart) {
        this.candidature = candidature;
        this.employeur = employeur;
        this.pays = pays;
        this.ville = ville;
        this.posteActuel = posteActuel;
        this.intitulePoste = intitulePoste;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nomSuperviseur = nomSuperviseur;
        this.titreSuperviseur = titreSuperviseur;
        this.telephoneSuperviseur = telephoneSuperviseur;
        this.emailSuperviseur = emailSuperviseur;
        this.contactAutorise = contactAutorise;
        this.motifNonContact = motifNonContact;
        this.motifDepart = motifDepart;
    }
}
