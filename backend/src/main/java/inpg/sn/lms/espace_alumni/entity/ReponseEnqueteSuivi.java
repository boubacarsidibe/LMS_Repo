package inpg.sn.lms.espace_alumni.entity;

import java.time.LocalDateTime;

import inpg.sn.lms.auth.entity.AlumniProfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Réponse d'un alumni à une {@link EnqueteSuivi} (TDR §G, besoin spécifique :
 * « recueillir des retours sur l'application des compétences dans leur
 * travail »). Deux dimensions notées sur 5, explicitement demandées par le
 * TDR : l'application des compétences acquises, et l'impact sur la carrière.
 */
@Entity
@Table(name = "reponses_enquete_suivi",
        uniqueConstraints = @UniqueConstraint(columnNames = { "enquete_id", "alumni_profil_id" }))
@Getter
@Setter
@NoArgsConstructor
public class ReponseEnqueteSuivi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquete_id", nullable = false)
    private EnqueteSuivi enquete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alumni_profil_id", nullable = false)
    private AlumniProfil alumniProfil;

    @Column(name = "note_application_competences", nullable = false)
    private int noteApplicationCompetences;

    @Column(name = "note_impact_carriere", nullable = false)
    private int noteImpactCarriere;

    @Column(length = 2000)
    private String commentaire;

    @Column(name = "date_reponse", nullable = false)
    private LocalDateTime dateReponse = LocalDateTime.now();

    public ReponseEnqueteSuivi(EnqueteSuivi enquete, AlumniProfil alumniProfil, int noteApplicationCompetences,
            int noteImpactCarriere, String commentaire) {
        this.enquete = enquete;
        this.alumniProfil = alumniProfil;
        this.noteApplicationCompetences = noteApplicationCompetences;
        this.noteImpactCarriere = noteImpactCarriere;
        this.commentaire = commentaire;
        this.dateReponse = LocalDateTime.now();
    }
}
