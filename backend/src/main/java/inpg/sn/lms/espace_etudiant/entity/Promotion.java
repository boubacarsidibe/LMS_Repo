package inpg.sn.lms.espace_etudiant.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Cohorte d'étudiants suivant une même {@link Formation} sur une période donnée.
 *
 * <p>Le cahier des charges demande de « renseigner / archiver les informations
 * concernant chaque promotion (programme/calendrier, y compris les activités
 * parascolaires, sorties et visites, cours et formateurs) » : les champs libres
 * {@code programme} et {@code activitesParascolaires} portent cette mémoire, le
 * reste étant dérivé de la formation rattachée.
 */
@Entity
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String libelle;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    /** Programme pédagogique de la promotion, saisi librement par l'INPG. */
    @Column(columnDefinition = "text")
    private String programme;

    /** Activités parascolaires, sorties et visites — explicitement demandées au TDR. */
    @Column(columnDefinition = "text")
    private String activitesParascolaires;

    /**
     * Formation suivie par la promotion. Nullable car les promotions déjà en base
     * ont été créées avant l'introduction du lien (ddl-auto=update ne peut pas
     * rétro-remplir une colonne NOT NULL).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formation_id")
    private Formation formation;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getProgramme() {
        return programme;
    }

    public void setProgramme(String programme) {
        this.programme = programme;
    }

    public String getActivitesParascolaires() {
        return activitesParascolaires;
    }

    public void setActivitesParascolaires(String activitesParascolaires) {
        this.activitesParascolaires = activitesParascolaires;
    }

    public Formation getFormation() {
        return formation;
    }

    public void setFormation(Formation formation) {
        this.formation = formation;
    }

    /** Vraie si la promotion est en cours à la date du jour. */
    public boolean estEnCours() {
        LocalDate aujourdhui = LocalDate.now();
        return (dateDebut == null || !aujourdhui.isBefore(dateDebut))
                && (dateFin == null || !aujourdhui.isAfter(dateFin));
    }
}
