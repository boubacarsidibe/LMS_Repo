package inpg.sn.lms.espace_etudiant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Une ligne de relevé : un cours, son coefficient, la moyenne obtenue.
 *
 * <p>Le code et le titre du cours sont <strong>recopiés</strong>, sans clé étrangère
 * vers {@code cours}. Un relevé délivré atteste d'un parcours : il doit survivre à la
 * suppression du cours, et ne pas se réécrire si le cours est renommé ou si son
 * coefficient change l'année suivante.
 */
@Entity
@Table(name = "releve_ligne")
public class ReleveLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "releve_id", nullable = false)
    private ReleveNotes releve;

    @Column(name = "cours_code", nullable = false)
    private String coursCode;

    @Column(name = "cours_titre", nullable = false)
    private String coursTitre;

    @Column(nullable = false)
    private double coefficient = 1;

    /** {@code null} si le cours n'avait aucune note à la délivrance. */
    @Column
    private Double moyenne;

    public Long getId() {
        return id;
    }

    public ReleveNotes getReleve() {
        return releve;
    }

    public void setReleve(ReleveNotes releve) {
        this.releve = releve;
    }

    public String getCoursCode() {
        return coursCode;
    }

    public void setCoursCode(String coursCode) {
        this.coursCode = coursCode;
    }

    public String getCoursTitre() {
        return coursTitre;
    }

    public void setCoursTitre(String coursTitre) {
        this.coursTitre = coursTitre;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    public Double getMoyenne() {
        return moyenne;
    }

    public void setMoyenne(Double moyenne) {
        this.moyenne = moyenne;
    }
}
