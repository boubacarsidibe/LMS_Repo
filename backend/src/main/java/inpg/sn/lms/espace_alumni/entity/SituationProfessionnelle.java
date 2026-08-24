package inpg.sn.lms.espace_alumni.entity;

import java.time.LocalDate;
import inpg.sn.lms.auth.entity.AlumniProfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une ligne de l'historique professionnel d'un alumni.
 *
 * <p>Historique, pas un simple champ « poste actuel » : un alumni change
 * d'employeur plusieurs fois, et {@code dateFin} nulle signale le poste en cours.
 * Même principe que {@code ReleveLigne} pour un relevé de notes — figer chaque
 * situation successive plutôt que n'en garder qu'une.
 */
@Entity
@Table(name = "situations_professionnelles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SituationProfessionnelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alumni_profil_id", nullable = false)
    private AlumniProfil alumniProfil;

    @Column(name = "intitule_poste", nullable = false)
    private String intitulePoste;

    @Column(nullable = false)
    private String entreprise;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /** Nulle = poste actuellement occupé. */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    public SituationProfessionnelle(AlumniProfil alumniProfil, String intitulePoste, String entreprise,
            LocalDate dateDebut, LocalDate dateFin) {
        this.alumniProfil = alumniProfil;
        this.intitulePoste = intitulePoste;
        this.entreprise = entreprise;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }
}
